package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * The fulfilled autofill data to be loaded into the a fill response.
 */
data class FilledData(
    val filledItems: List<FilledItem>,
    val ignoreAutofillIds: List<AutofillId>,
)
