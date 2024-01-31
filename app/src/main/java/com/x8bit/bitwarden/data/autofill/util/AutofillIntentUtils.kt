@file:OmitFromCoverage

package com.x8bit.bitwarden.data.autofill.util

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.service.autofill.Dataset
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.AutofillTotpCopyActivity
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.model.AutofillTotpCopyData
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.getSafeParcelableExtra

private const val AUTOFILL_SAVE_ITEM_DATA_KEY = "autofill-save-item-data"
private const val AUTOFILL_SELECTION_DATA_KEY = "autofill-selection-data"
private const val AUTOFILL_TOTP_COPY_DATA_KEY = "autofill-totp-copy-data"

/**
 * Creates an [Intent] in order to send the user to a manual selection process for autofill.
 */
fun createAutofillSelectionIntent(
    context: Context,
    type: AutofillSelectionData.Type,
    uri: String?,
): Intent =
    Intent(
        context,
        MainActivity::class.java,
    )
        .apply {
            putExtra(
                AUTOFILL_SELECTION_DATA_KEY,
                AutofillSelectionData(
                    type = type,
                    uri = uri,
                ),
            )
        }

/**
 * Creates an [IntentSender] built with the data required for performing a TOTP copying during
 * the autofill flow.
 */
fun createTotpCopyIntentSender(
    cipherId: String,
    context: Context,
): IntentSender {
    val intent = Intent(
        context,
        AutofillTotpCopyActivity::class.java,
    )
        .apply {
            putExtra(
                AUTOFILL_TOTP_COPY_DATA_KEY,
                AutofillTotpCopyData(
                    cipherId = cipherId,
                ),
            )
        }

    return PendingIntent
        .getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT.toPendingIntentMutabilityFlag(),
        )
        .intentSender
}

/**
 * Creates an [Intent] in order to start the cipher saving process during the autofill flow.
 */
fun createAutofillSavedItemIntent(
    autofillAppInfo: AutofillAppInfo,
    autofillSaveItem: AutofillSaveItem,
): Intent =
    Intent(
        autofillAppInfo.context,
        MainActivity::class.java,
    )
        .apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AUTOFILL_SAVE_ITEM_DATA_KEY, autofillSaveItem)
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
    this.getSafeParcelableExtra(AUTOFILL_SELECTION_DATA_KEY)

/**
 * Checks if the given [Intent] contains data for TOTP copying. The [AutofillTotpCopyData] will be
 * returned when present.
 */
fun Intent.getTotpCopyIntentOrNull(): AutofillTotpCopyData? =
    this.getSafeParcelableExtra(AUTOFILL_TOTP_COPY_DATA_KEY)
