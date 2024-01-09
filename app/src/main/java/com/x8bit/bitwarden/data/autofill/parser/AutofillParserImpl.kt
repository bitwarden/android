package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.util.toAutofillView

/**
 * The default [AutofillParser] implementation for the app. This is a tool for parsing autofill data
 * from the OS into domain models.
 */
class AutofillParserImpl : AutofillParser {
    override fun parse(assistStructure: AssistStructure): AutofillRequest {
        // Parse the `assistStructure` into internal models.
        val traversalData = assistStructure.traverse()
        // Flatten the autofill views for processing.
        val autofillViews = traversalData
            .map { it.autofillViews }
            .flatten()

        // Find the focused view (or indicate there is no fulfillment to be performed.)
        val focusedView = autofillViews
            .firstOrNull { it.isFocused }
            ?: return AutofillRequest.Unfillable

        // Choose the first focused partition of data for fulfillment.
        val partition = when (focusedView) {
            is AutofillView.Card -> {
                AutofillPartition.Card(
                    views = autofillViews.filterIsInstance<AutofillView.Card>(),
                )
            }

            is AutofillView.Identity -> {
                AutofillPartition.Identity(
                    views = autofillViews.filterIsInstance<AutofillView.Identity>(),
                )
            }

            is AutofillView.Login -> {
                AutofillPartition.Login(
                    views = autofillViews.filterIsInstance<AutofillView.Login>(),
                )
            }
        }
        // Flatten the ignorable autofill ids.
        val ignoreAutofillIds = traversalData
            .map { it.ignoreAutofillIds }
            .flatten()

        return AutofillRequest.Fillable(
            ignoreAutofillIds = ignoreAutofillIds,
            partition = partition,
        )
    }
}

/**
 * Traverse the [AssistStructure] and convert it into a list of [ViewNodeTraversalData]s.
 */
private fun AssistStructure.traverse(): List<ViewNodeTraversalData> =
    (0 until windowNodeCount)
        .map { getWindowNodeAt(it) }
        .mapNotNull { windowNode -> windowNode.rootViewNode?.traverse() }

/**
 * Recursively traverse this [AssistStructure.ViewNode] and all of its descendants. Convert the
 * data into [ViewNodeTraversalData].
 */
private fun AssistStructure.ViewNode.traverse(): ViewNodeTraversalData {
    // Set up mutable lists for collecting valid AutofillViews and ignorable view ids.
    val mutableAutofillViewList: MutableList<AutofillView> = mutableListOf()
    val mutableIgnoreAutofillIdList: MutableList<AutofillId> = mutableListOf()

    // Try converting this `ViewNode` into an `AutofillView`. If a valid instance is returned, add
    // it to the list. Otherwise, ignore the `AutofillId` associated with this `ViewNode`.
    toAutofillView()
        ?.run(mutableAutofillViewList::add)
        ?: autofillId?.run(mutableIgnoreAutofillIdList::add)

    // Recursively traverse all of this view node's children.
    for (i in 0 until childCount) {
        // Extract the traversal data from each child view node and add it to the lists.
        getChildAt(i)
            .traverse()
            .let { viewNodeTraversalData ->
                viewNodeTraversalData.autofillViews.forEach(mutableAutofillViewList::add)
                viewNodeTraversalData.ignoreAutofillIds.forEach(mutableIgnoreAutofillIdList::add)
            }
    }

    // Build a new traversal data structure with this view node's data, and that of all of its
    // descendant's.
    return ViewNodeTraversalData(
        autofillViews = mutableAutofillViewList,
        ignoreAutofillIds = mutableIgnoreAutofillIdList,
    )
}

/**
 * A convenience data structure for view node traversal.
 */
private data class ViewNodeTraversalData(
    val autofillViews: List<AutofillView>,
    val ignoreAutofillIds: List<AutofillId>,
)
