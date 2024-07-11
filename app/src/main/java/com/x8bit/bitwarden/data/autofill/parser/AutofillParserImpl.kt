package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillRequest
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.data.autofill.util.buildPackageNameOrNull
import com.x8bit.bitwarden.data.autofill.util.buildUriOrNull
import com.x8bit.bitwarden.data.autofill.util.getInlinePresentationSpecs
import com.x8bit.bitwarden.data.autofill.util.getMaxInlineSuggestionsCount
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import com.x8bit.bitwarden.data.autofill.util.website
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * A list of URIs that should never be autofilled.
 */
private val BLOCK_LISTED_URIS: List<String> = listOf(
    "androidapp://android",
    "androidapp://com.android.settings",
    "androidapp://com.x8bit.bitwarden",
    "androidapp://com.oneplus.applocker",
)

/**
 * The default [AutofillParser] implementation for the app. This is a tool for parsing autofill data
 * from the OS into domain models.
 */
class AutofillParserImpl(
    private val settingsRepository: SettingsRepository,
) : AutofillParser {
    override fun parse(
        autofillAppInfo: AutofillAppInfo,
        fillRequest: FillRequest,
    ): AutofillRequest =
        // Attempt to get the most recent autofill context.
        fillRequest
            .fillContexts
            .lastOrNull()
            ?.structure
            ?.let { assistStructure ->
                parseInternal(
                    assistStructure = assistStructure,
                    autofillAppInfo = autofillAppInfo,
                    fillRequest = fillRequest,
                )
            }
            ?: AutofillRequest.Unfillable

    override fun parse(
        autofillAppInfo: AutofillAppInfo,
        assistStructure: AssistStructure,
    ): AutofillRequest =
        parseInternal(
            assistStructure = assistStructure,
            autofillAppInfo = autofillAppInfo,
            fillRequest = null,
        )

    /**
     * Parse the [AssistStructure] into an [AutofillRequest].
     */
    private fun parseInternal(
        assistStructure: AssistStructure,
        autofillAppInfo: AutofillAppInfo,
        fillRequest: FillRequest?,
    ): AutofillRequest {
        // Parse the `assistStructure` into internal models.
        val traversalDataList = assistStructure.traverse()
        // Flatten the autofill views for processing.
        val autofillViews = traversalDataList
            .map { it.autofillViews }
            .flatten()

        // Find the focused view.
        val focusedView = autofillViews.firstOrNull { it.data.isFocused }

        val packageName = traversalDataList.buildPackageNameOrNull(
            assistStructure = assistStructure,
        )
        val uri = traversalDataList.buildUriOrNull(
            packageName = packageName,
        )

        val blockListedURIs = settingsRepository.blockedAutofillUris + BLOCK_LISTED_URIS
        if (focusedView == null || blockListedURIs.contains(uri)) {
            // The view is unfillable if there are no focused views or the URI is block listed.
            return AutofillRequest.Unfillable
        }

        // Choose the first focused partition of data for fulfillment.
        val partition = when (focusedView) {
            is AutofillView.Card -> {
                AutofillPartition.Card(
                    views = autofillViews.filterIsInstance<AutofillView.Card>(),
                )
            }

            is AutofillView.Login -> {
                AutofillPartition.Login(
                    views = autofillViews.filterIsInstance<AutofillView.Login>(),
                )
            }

            is AutofillView.Unused -> {
                // The view is unfillable since the field is not meant to be used for autofill.
                return AutofillRequest.Unfillable
            }
        }
        // Flatten the ignorable autofill ids.
        val ignoreAutofillIds = traversalDataList
            .map { it.ignoreAutofillIds }
            .flatten()

        // Get inline information if available
        val isInlineAutofillEnabled = settingsRepository.isInlineAutofillEnabled
        val maxInlineSuggestionsCount = fillRequest.getMaxInlineSuggestionsCount(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = isInlineAutofillEnabled,
        )
        val inlinePresentationSpecs = fillRequest.getInlinePresentationSpecs(
            autofillAppInfo = autofillAppInfo,
            isInlineAutofillEnabled = isInlineAutofillEnabled,
        )

        return AutofillRequest.Fillable(
            inlinePresentationSpecs = inlinePresentationSpecs,
            ignoreAutofillIds = ignoreAutofillIds,
            maxInlineSuggestionsCount = maxInlineSuggestionsCount,
            packageName = packageName,
            partition = partition,
            uri = uri,
        )
    }
}

/**
 * Traverse the [AssistStructure] and convert it into a list of [ViewNodeTraversalData]s.
 */
private fun AssistStructure.traverse(): List<ViewNodeTraversalData> =
    (0 until windowNodeCount)
        .map { getWindowNodeAt(it) }
        .mapNotNull { windowNode ->
            windowNode
                .rootViewNode
                ?.traverse()
                ?.updateForMissingUsernameFields()
        }

/**
 * This helper function updates the [ViewNodeTraversalData] if necessary for missing username
 * fields that could have been missed. If the current `ViewNodeTraversalData` contains password
 * fields but no username fields, we check to see if there are any unused fields directly above
 * the password fields and we assume that those are the missing username fields.
 */
private fun ViewNodeTraversalData.updateForMissingUsernameFields(): ViewNodeTraversalData {
    val passwordPositions = this.autofillViews.mapIndexedNotNull { index, autofillView ->
        (autofillView as? AutofillView.Login.Password)?.let { index }
    }
    return if (passwordPositions.any() &&
        this.autofillViews.none { it is AutofillView.Login.Username }
    ) {
        val updatedAutofillViews = autofillViews.mapIndexed { index, autofillView ->
            if (autofillView is AutofillView.Unused && passwordPositions.contains(index + 1)) {
                AutofillView.Login.Username(data = autofillView.data)
            } else {
                autofillView
            }
        }
        val previousUnusedIds = autofillViews
            .filterIsInstance<AutofillView.Unused>()
            .map { it.data.autofillId }
            .toSet()
        val currentUnusedIds = updatedAutofillViews
            .filterIsInstance<AutofillView.Unused>()
            .map { it.data.autofillId }
            .toSet()
        val unignoredAutofillIds = previousUnusedIds - currentUnusedIds
        this.copy(
            autofillViews = updatedAutofillViews,
            ignoreAutofillIds = this.ignoreAutofillIds - unignoredAutofillIds,
        )
    } else {
        // We already have username fields available or there are no password fields, so no need
        // to search for them.
        this
    }
}

/**
 * Recursively traverse this [AssistStructure.ViewNode] and all of its descendants. Convert the
 * data into [ViewNodeTraversalData].
 */
private fun AssistStructure.ViewNode.traverse(): ViewNodeTraversalData {
    // Set up mutable lists for collecting valid AutofillViews and ignorable view ids.
    val mutableAutofillViewList: MutableList<AutofillView> = mutableListOf()
    val mutableIgnoreAutofillIdList: MutableList<AutofillId> = mutableListOf()
    var idPackage: String? = this.idPackage
    var website: String? = this.website

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

                // Get the first non-null idPackage.
                if (idPackage.isNullOrBlank() &&
                    // OS sometimes defaults node.idPackage to "android", which is not a valid
                    // package name so it is ignored to prevent auto-filling unknown applications.
                    viewNodeTraversalData.idPackage?.equals("android") == false
                ) {
                    idPackage = viewNodeTraversalData.idPackage
                }
                // Get the first non-null website.
                if (website == null) {
                    website = viewNodeTraversalData.website
                }
            }
    }

    // Build a new traversal data structure with this view node's data, and that of all of its
    // descendant's.
    return ViewNodeTraversalData(
        autofillViews = mutableAutofillViewList,
        idPackage = idPackage,
        ignoreAutofillIds = mutableIgnoreAutofillIdList,
        website = website,
    )
}
