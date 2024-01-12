package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * A convenience data structure for view node traversal.
 */
data class ViewNodeTraversalData(
    val autofillViews: List<AutofillView>,
    val ignoreAutofillIds: List<AutofillId>,
)
