package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * The parsed autofill request.
 */
sealed class AutofillRequest {
    /**
     * An autofill request that is fillable. This means it has [partition] of data that can be
     * fulfilled.
     */
    data class Fillable(
        val ignoreAutofillIds: List<AutofillId>,
        val partition: AutofillPartition,
        val uri: String?,
    ) : AutofillRequest()

    /**
     * An autofill request that is unfillable.
     */
    data object Unfillable : AutofillRequest()
}
