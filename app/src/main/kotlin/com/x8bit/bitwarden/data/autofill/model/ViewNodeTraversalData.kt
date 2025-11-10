package com.x8bit.bitwarden.data.autofill.model

import android.view.autofill.AutofillId

/**
 * A convenience data structure for view node traversal.
 *
 * @param autofillViews The list of views we care about for autofilling.
 * @param idPackage The package id for this view, if there is one.
 * @param urlBarWebsites The website associated with the URL bar view.
 * @param ignoreAutofillIds The list of [AutofillId]s that should be ignored in the fill response.
 */
data class ViewNodeTraversalData(
    val autofillViews: List<AutofillView>,
    val idPackage: String?,
    val urlBarWebsites: List<String>,
    val ignoreAutofillIds: List<AutofillId>,
)
