package com.x8bit.bitwarden.data.autofill.util

import android.annotation.SuppressLint
import android.content.IntentSender
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Presentations
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.ui.autofill.buildAutofillRemoteViews
import com.x8bit.bitwarden.ui.autofill.util.createCipherInlinePresentationOrNull

/**
 * Build a [Dataset] to represent the [FilledPartition]. This dataset includes an overlay UI
 * presentation for each filled item. If an [authIntentSender] is present, add it to the dataset.
 */
@SuppressLint("NewApi")
fun FilledPartition.buildDataset(
    authIntentSender: IntentSender?,
    autofillAppInfo: AutofillAppInfo,
): Dataset {
    val remoteViewsPlaceholder = buildAutofillRemoteViews(
        autofillAppInfo = autofillAppInfo,
        autofillCipher = autofillCipher,
    )
    val datasetBuilder = Dataset.Builder()

    authIntentSender
        ?.let { intentSender ->
            datasetBuilder.setAuthentication(intentSender)
        }

    if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        applyToDatasetPostTiramisu(
            autofillAppInfo = autofillAppInfo,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViewsPlaceholder,
        )
    } else {
        buildDatasetPreTiramisu(
            autofillAppInfo = autofillAppInfo,
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViewsPlaceholder,
        )
    }

    return datasetBuilder.build()
}

/**
 * Apply this [FilledPartition] to the [datasetBuilder] on devices running OS version Tiramisu or
 * greater.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun FilledPartition.applyToDatasetPostTiramisu(
    autofillAppInfo: AutofillAppInfo,
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    val presentationBuilder = Presentations.Builder()
    inlinePresentationSpec
        ?.createCipherInlinePresentationOrNull(
            autofillAppInfo = autofillAppInfo,
            autofillCipher = autofillCipher,
        )
        ?.let { inlinePresentation ->
            presentationBuilder.setInlinePresentation(inlinePresentation)
        }

    val presentation = presentationBuilder
        .setMenuPresentation(remoteViews)
        .build()

    filledItems.forEach { filledItem ->
        filledItem.applyToDatasetPostTiramisu(
            datasetBuilder = datasetBuilder,
            presentations = presentation,
        )
    }
}

/**
 * Apply this [FilledPartition] to the [datasetBuilder] on devices running OS versions that predate
 * Tiramisu.
 */
@Suppress("DEPRECATION")
@SuppressLint("NewApi")
private fun FilledPartition.buildDatasetPreTiramisu(
    autofillAppInfo: AutofillAppInfo,
    datasetBuilder: Dataset.Builder,
    remoteViews: RemoteViews,
) {
    if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.R) {
        inlinePresentationSpec
            ?.createCipherInlinePresentationOrNull(
                autofillAppInfo = autofillAppInfo,
                autofillCipher = autofillCipher,
            )
            ?.let { inlinePresentation ->
                datasetBuilder.setInlinePresentation(inlinePresentation)
            }
    }

    filledItems.forEach { filledItem ->
        filledItem.applyToDatasetPreTiramisu(
            datasetBuilder = datasetBuilder,
            remoteViews = remoteViews,
        )
    }
}
