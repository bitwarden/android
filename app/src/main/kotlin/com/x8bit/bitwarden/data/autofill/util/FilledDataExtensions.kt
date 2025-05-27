package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Presentations
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.ui.autofill.buildVaultItemAutofillRemoteViews
import com.x8bit.bitwarden.ui.autofill.util.createVaultItemInlinePresentationOrNull
import kotlin.random.Random

/**
 * Returns all the possible [AutofillId]s that were potentially fillable for the given [FilledData].
 */
val FilledData.fillableAutofillIds: List<AutofillId>
    get() = this.originalPartition.views.map { it.data.autofillId }

/**
 * Builds a [Dataset] for the Vault item.
 */
@SuppressLint("NewApi")
fun FilledData.buildVaultItemDataset(
    autofillAppInfo: AutofillAppInfo,
): Dataset {
    val intent = createAutofillSelectionIntent(
        context = autofillAppInfo.context,
        framework = AutofillSelectionData.Framework.AUTOFILL,
        type = when (this.originalPartition) {
            is AutofillPartition.Card -> AutofillSelectionData.Type.CARD
            is AutofillPartition.Login -> AutofillSelectionData.Type.LOGIN
        },
        uri = this.uri,
    )

    val pendingIntent = PendingIntent
        .getActivity(
            autofillAppInfo.context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT.toPendingIntentMutabilityFlag(),
        )

    val remoteViewsForOverlay = buildVaultItemAutofillRemoteViews(
        autofillAppInfo = autofillAppInfo,
        isLocked = this.isVaultLocked,
    )

    val filledItems = this
        .fillableAutofillIds
        .map {
            FilledItem(
                autofillId = it,
                // A placeholder value must be used for now, but this are temporary. It will get
                // reset when a real value is chosen by the user in the "authentication activity"
                // that is launched as part of the PendingIntent.
                value = AutofillValue.forText("PLACEHOLDER"),
            )
        }
    val inlinePresentationSpec = this.vaultItemInlinePresentationSpec
    return Dataset.Builder()
        .setAuthentication(pendingIntent.intentSender)
        .apply {
            if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                addVaultItemDataPostTiramisu(
                    autofillAppInfo = autofillAppInfo,
                    pendingIntent = pendingIntent,
                    remoteViews = remoteViewsForOverlay,
                    filledItems = filledItems,
                    inlinePresentationSpec = inlinePresentationSpec,
                    isLocked = isVaultLocked,
                )
            } else {
                addVaultItemDataPreTiramisu(
                    autofillAppInfo = autofillAppInfo,
                    pendingIntent = pendingIntent,
                    remoteViews = remoteViewsForOverlay,
                    filledItems = filledItems,
                    inlinePresentationSpec = inlinePresentationSpec,
                    isLocked = isVaultLocked,
                )
            }
        }
        .build()
}

/**
 * Adds the Vault data to the given [Dataset.Builder] for post-Tiramisu versions.
 */
@Suppress("LongParameterList")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Dataset.Builder.addVaultItemDataPostTiramisu(
    autofillAppInfo: AutofillAppInfo,
    pendingIntent: PendingIntent,
    remoteViews: RemoteViews,
    filledItems: List<FilledItem>,
    inlinePresentationSpec: InlinePresentationSpec?,
    isLocked: Boolean,
): Dataset.Builder {
    val presentationBuilder = Presentations.Builder()
    inlinePresentationSpec
        ?.createVaultItemInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            pendingIntent = pendingIntent,
            isLocked = isLocked,
        )
        ?.let { inlinePresentation ->
            presentationBuilder.setInlinePresentation(inlinePresentation)
        }
    val presentation = presentationBuilder
        .setMenuPresentation(remoteViews)
        .build()
    filledItems
        .forEach {
            it.applyToDatasetPostTiramisu(
                datasetBuilder = this,
                presentations = presentation,
            )
        }
    return this
}

/**
 * Adds the Vault data to the given [Dataset.Builder] for pre-Tiramisu versions.
 */
@Suppress("DEPRECATION", "LongParameterList")
@SuppressLint("NewApi")
private fun Dataset.Builder.addVaultItemDataPreTiramisu(
    autofillAppInfo: AutofillAppInfo,
    pendingIntent: PendingIntent,
    remoteViews: RemoteViews,
    filledItems: List<FilledItem>,
    inlinePresentationSpec: InlinePresentationSpec?,
    isLocked: Boolean,
): Dataset.Builder {
    if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.R) {
        inlinePresentationSpec
            ?.createVaultItemInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                pendingIntent = pendingIntent,
                isLocked = isLocked,
            )
            ?.let { inlinePresentation ->
                this.setInlinePresentation(inlinePresentation)
            }
    }
    filledItems
        .forEach {
            it.applyToDatasetPreTiramisu(
                datasetBuilder = this,
                remoteViews = remoteViews,
            )
        }
    return this
}
