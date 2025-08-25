@file:OmitFromCoverage

package com.x8bit.bitwarden.data.autofill.util

import android.app.Activity
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.service.autofill.Dataset
import android.view.autofill.AutofillManager
import androidx.core.os.bundleOf
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.toPendingIntentMutabilityFlag
import com.bitwarden.ui.platform.util.getSafeParcelableExtra
import com.x8bit.bitwarden.AutofillCallbackActivity
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCallbackData
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import kotlin.random.Random

private const val AUTOFILL_SAVE_ITEM_DATA_KEY = "autofill-save-item-data"
private const val AUTOFILL_SELECTION_DATA_KEY = "autofill-selection-data"
private const val AUTOFILL_CALLBACK_DATA_KEY = "autofill-callback-data"
private const val AUTOFILL_BUNDLE_KEY = "autofill-bundle-key"

/**
 * Creates an [Intent] in order to send the user to a manual selection process for autofill.
 */
fun createAutofillSelectionIntent(
    context: Context,
    framework: AutofillSelectionData.Framework,
    type: AutofillSelectionData.Type,
    uri: String?,
): Intent =
    Intent(context, MainActivity::class.java)
        .apply {
            // This helps prevent a crash when using the accessibility framework
            if (framework == AutofillSelectionData.Framework.ACCESSIBILITY) {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            putExtra(
                AUTOFILL_BUNDLE_KEY,
                bundleOf(
                    AUTOFILL_SELECTION_DATA_KEY to AutofillSelectionData(
                        framework = framework,
                        type = type,
                        uri = uri,
                    ),
                ),
            )
        }

/**
 * Creates an [IntentSender] built with the data required for performing an Autofill callback
 * during the autofill flow.
 */
fun createAutofillCallbackIntentSender(
    cipherId: String,
    context: Context,
): IntentSender {
    val intent = Intent(
        context,
        AutofillCallbackActivity::class.java,
    )
        .putExtra(
            AUTOFILL_BUNDLE_KEY,
            bundleOf(
                AUTOFILL_CALLBACK_DATA_KEY to AutofillCallbackData(cipherId = cipherId),
            ),
        )
    return PendingIntent
        .getActivity(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT.toPendingIntentMutabilityFlag(),
        )
        .intentSender
}

/**
 * Creates an [IntentSender] in order to start the cipher saving process during the autofill flow.
 */
fun createAutofillSavedItemIntentSender(
    autofillAppInfo: AutofillAppInfo,
    autofillSaveItem: AutofillSaveItem,
): IntentSender {
    val intent = Intent(
        autofillAppInfo.context,
        MainActivity::class.java,
    )
        .apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AUTOFILL_SAVE_ITEM_DATA_KEY, autofillSaveItem)
        }

    return PendingIntent
        .getActivity(
            autofillAppInfo.context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT.toPendingIntentMutabilityFlag(),
        )
        .intentSender
}

/**
 * Creates an [Intent] in order to specify that there is a successful selection during a manual
 * autofill process.
 */
fun createAutofillSelectionResultIntent(
    dataset: Dataset,
): Intent =
    Intent()
        .apply {
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
        }

/**
 * Checks if the given [Intent] contains an [AssistStructure] related to an ongoing manual autofill
 * selection process.
 */
fun Intent.getAutofillAssistStructureOrNull(): AssistStructure? =
    this.getSafeParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)

/**
 * Checks if the given [Intent] contains an [AutofillSaveItem] related to an ongoing save item
 * process.
 */
fun Intent.getAutofillSaveItemOrNull(): AutofillSaveItem? =
    this.getSafeParcelableExtra(AUTOFILL_SAVE_ITEM_DATA_KEY)

/**
 * Checks if the given [Intent] contains data about an ongoing manual autofill selection process.
 * The [AutofillSelectionData] will be returned when present.
 */
fun Intent.getAutofillSelectionDataOrNull(): AutofillSelectionData? =
    getBundleExtra(AUTOFILL_BUNDLE_KEY)
        ?.getSafeParcelableExtra(AUTOFILL_SELECTION_DATA_KEY)

/**
 * Checks if the given [Intent] contains Autofill callback data. The [AutofillCallbackData] will be
 * returned when present.
 */
fun Intent.getAutofillCallbackIntentOrNull(): AutofillCallbackData? =
    getBundleExtra(AUTOFILL_BUNDLE_KEY)
        ?.getSafeParcelableExtra(AUTOFILL_CALLBACK_DATA_KEY)

/**
 * Checks if the given [Activity] was created for Autofill. This is useful to avoid locking the
 * vault if one of the Autofill services starts the only instance of the [MainActivity].
 */
val Activity.createdForAutofill: Boolean
    get() = intent.getAutofillSelectionDataOrNull() != null ||
        intent.getAutofillSaveItemOrNull() != null ||
        intent.getAutofillAssistStructureOrNull() != null
