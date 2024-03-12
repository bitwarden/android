package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId
import android.widget.inline.InlinePresentationSpec

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
        val inlinePresentationSpecs: List<InlinePresentationSpec>?,
        val maxInlineSuggestionsCount: Int,
        val packageName: String?,
        val partition: AutofillPartition,
        val uri: String?,
    ) : AutofillRequest()

    /**
     * An autofill request that is unfillable.
     */
    data object Unfillable : AutofillRequest()
}
