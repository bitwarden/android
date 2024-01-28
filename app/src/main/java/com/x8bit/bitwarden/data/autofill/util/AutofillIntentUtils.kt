@file:OmitFromCoverage

package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.service.autofill.Dataset
import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.getSafeParcelableExtra

private const val AUTOFILL_SELECTION_DATA_KEY = "autofill-selection-data"

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
 * Checks if the given [Intent] contains data about an ongoing manual autofill selection process.
 * The [AutofillSelectionData] will be returned when present.
 */
fun Intent.getAutofillSelectionDataOrNull(): AutofillSelectionData? =
    this.getSafeParcelableExtra(AUTOFILL_SELECTION_DATA_KEY)
