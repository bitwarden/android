@file:OmitFromCoverage

package com.x8bit.bitwarden.data.autofill.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

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
 * Checks if the given [Intent] contains data about an ongoing manual autofill selection process.
 * The [AutofillSelectionData] will be returned when present.
 */
fun Intent.getAutofillSelectionDataOrNull(): AutofillSelectionData? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableExtra(AUTOFILL_SELECTION_DATA_KEY, AutofillSelectionData::class.java)
    } else {
        this.getParcelableExtra(AUTOFILL_SELECTION_DATA_KEY)
    }
}
