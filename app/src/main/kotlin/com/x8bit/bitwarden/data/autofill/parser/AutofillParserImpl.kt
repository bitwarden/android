package com.x8bit.bitwarden.data.autofill.parser

import android.app.assist.AssistStructure
import android.service.autofill.FillRequest
import android.view.autofill.AutofillId
import androidx.core.net.toUri
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.autofill.manager.FillAssistManager
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import com.x8bit.bitwarden.data.autofill.model.ViewNodeTraversalData
import com.x8bit.bitwarden.data.autofill.util.buildPackageNameOrNull
import com.x8bit.bitwarden.data.autofill.util.buildUriOrNull
import com.x8bit.bitwarden.data.autofill.util.getInlinePresentationSpecs
import com.x8bit.bitwarden.data.autofill.util.getMaxInlineSuggestionsCount
import com.x8bit.bitwarden.data.autofill.util.toAutofillView
import com.x8bit.bitwarden.data.autofill.util.toFillAssistView
import com.x8bit.bitwarden.data.autofill.util.website
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import timber.log.Timber

/**
 * A list of URIs that should never be autofilled.
 */
private val BLOCK_LISTED_URIS: List<String> = listOf(
    "androidapp://android",
    "androidapp://com.android.settings",
    "androidapp://com.x8bit.bitwarden",
    "androidapp://com.x8bit.bitwarden.beta",
    "androidapp://com.x8bit.bitwarden.dev",
    "androidapp://com.oneplus.applocker",
)

/**
 * A map of package ids and the known associated id entry for their url bar.
 */
private val URL_BARS: Map<String, String> = mapOf(
    // Edge Browser Variants
    "com.microsoft.emmx" to "url_bar",
    "com.microsoft.emmx.beta" to "url_bar",
    "com.microsoft.emmx.canary" to "url_bar",
    "com.microsoft.emmx.dev" to "url_bar",
    // Samsung Internet Browser Variants
    "com.sec.android.app.sbrowser" to "location_bar_edit_text",
    "com.sec.android.app.sbrowser.beta" to "location_bar_edit_text",
    // Opera Browser Variants
    "com.opera.browser" to "url_bar",
    "com.opera.browser.beta" to "url_bar",
    // Brave Browser Variants
    "com.brave.browser" to "url_bar",
    "com.brave.browser_beta" to "url_bar",
    "com.brave.browser_nightly" to "url_bar",
)

/**
 * A list of categories from Fill Assist that are used for [AutofillView.Login]
 */
private val LOGIN_FILL_ASSIST_CATEGORIES: List<String> = listOf(
    "account-login",
    "account-creation",
    "account-update",
)

/**
 * A list of categories from Fill Assist that are used for [AutofillView.Card]
 */
private val CARD_FILL_ASSIST_CATEGORIES: List<String> = listOf(
    "payment-card",
)

/**
 * The default [AutofillParser] implementation for the app. This is a tool for parsing autofill data
 * from the OS into domain models.
 */
class AutofillParserImpl(
    private val settingsRepository: SettingsRepository,
    private val fillAssistManager: FillAssistManager,
    private val featureFlagManager: FeatureFlagManager,
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
    @Suppress("LongMethod")
    private fun parseInternal(
        assistStructure: AssistStructure,
        autofillAppInfo: AutofillAppInfo,
        fillRequest: FillRequest?,
    ): AutofillRequest {
        Timber.d("Parsing AssistStructure -- ${fillRequest?.id}")

        // Pre-compute fill-assist rules once. If the feature flag is off or no rules are loaded,
        // allFillAssistRules is null and fill-assist matching is skipped during traversal.
        val allFillAssistRules: Map<String, List<FillAssistRules.HostRule>>? =
            fillAssistManager.getFillAssistRules()
                ?.takeIf { featureFlagManager.getFeatureFlag(FlagKey.FillAssistTargetingRules) }
                ?.hostRules

        // Single pass: collects both heuristic and fill-assist views simultaneously.
        val traversalDataList = assistStructure.traverse(allFillAssistRules = allFillAssistRules)
        val urlBarWebsite = traversalDataList
            .flatMap { it.urlBarWebsites }
            .firstOrNull()
        val autofillViews = traversalDataList.toAutofillViews(urlBarWebsite = urlBarWebsite)

        // Find the focused view, or fallback to the first fillable item on the screen (so
        // we at least have something to hook into)
        val focusedView = autofillViews
            .firstOrNull { it.data.isFocused }
            ?: autofillViews.firstOrNull()
            // The view is unfillable if there are no focused views.
            ?: return AutofillRequest.Unfillable

        val packageName = traversalDataList.buildPackageNameOrNull(
            assistStructure = assistStructure,
        )
        val uri = focusedView.buildUriOrNull(packageName = packageName)

        // The view is unfillable if the URI is block listed.
        if ((settingsRepository.blockedAutofillUris + BLOCK_LISTED_URIS).contains(uri)) {
            return AutofillRequest.Unfillable
        }

        val hostRules = traversalDataList.firstNotNullOfOrNull { it.fillAssistHostRules }
        val fillAssistViews = traversalDataList.flatMap { it.fillAssistViews }
        val effectiveViews = autofillViews.toEffectiveViews(
            fillAssistViews = fillAssistViews,
            hostRules = hostRules,
            focusedView = focusedView,
        )

        val effectiveFocusedView = effectiveViews
            .firstOrNull { it.data.isFocused }
            ?: effectiveViews.firstOrNull()
            ?: return AutofillRequest.Unfillable

        // Choose the first focused partition of data for fulfillment.
        val partition = when (effectiveFocusedView) {
            is AutofillView.Card -> {
                AutofillPartition.Card(
                    views = effectiveViews.filterIsInstance<AutofillView.Card>(),
                )
            }

            is AutofillView.Login -> {
                AutofillPartition.Login(
                    views = effectiveViews.filterIsInstance<AutofillView.Login>(),
                )
            }

            is AutofillView.Unused -> {
                // The view is unfillable since the field is not meant to be used for autofill.
                // This will never happen since we filter out all unused views above.
                return AutofillRequest.Unfillable
            }
        }
        // Flatten the ignorable autofill ids.
        val ignoreAutofillIds = traversalDataList
            .map { it.ignoreAutofillIds }
            .flatten()

        // Get inline information if available
        val isInlineAutofillEnabled = settingsRepository.isInlineAutofillEnabled
        Timber.d("Autofill request isInlineEnabled=$isInlineAutofillEnabled -- ${fillRequest?.id}")
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

    /**
     * Returns the effective [AutofillView] list for filling. Uses the pre-collected
     * [fillAssistViews] when the [hostRules] cover the current partition; otherwise returns
     * [this] (heuristic).
     */
    private fun List<AutofillView>.toEffectiveViews(
        fillAssistViews: List<AutofillView>,
        hostRules: List<FillAssistRules.HostRule>?,
        focusedView: AutofillView,
    ): List<AutofillView> {
        val rules = hostRules ?: return this
        val coversCurrentPartition = rules.any { rule ->
            when (focusedView) {
                is AutofillView.Card -> rule.category in CARD_FILL_ASSIST_CATEGORIES
                is AutofillView.Login -> rule.category in LOGIN_FILL_ASSIST_CATEGORIES
                is AutofillView.Unused -> false
            }
        }
        return if (coversCurrentPartition) fillAssistViews else this
    }
}

/**
 * Traverse the [AssistStructure] and convert it into a list of [ViewNodeTraversalData]s.
 * When [allFillAssistRules] is non-null, fill-assist views are also collected in a single pass.
 */
private fun AssistStructure.traverse(
    allFillAssistRules: Map<String, List<FillAssistRules.HostRule>>?,
): List<ViewNodeTraversalData> =
    (0 until windowNodeCount)
        .map { getWindowNodeAt(it) }
        .mapNotNull { windowNode ->
            windowNode
                .rootViewNode
                ?.traverse(
                    heuristicParentWebsite = null,
                    fillAssistParentWebsite = null,
                    parentFillAssistHostRules = null,
                    allFillAssistRules = allFillAssistRules,
                )
                ?.updateForMissingPasswordFields()
                ?.updateForMissingUsernameFields()
        }

/**
 * Assembles the [AutofillView] list from this [ViewNodeTraversalData] list.
 * Take only the autofill views from the node that currently has focus.
 * Then remove all the fields that cannot be filled with data.
 * We fall back to taking all the fillable views if nothing has focus.
 */
private fun List<ViewNodeTraversalData>.toAutofillViews(
    urlBarWebsite: String?,
): List<AutofillView> {
    val viewsLists = map { it.autofillViews }
    return (viewsLists
        .filter { views -> views.any { it.data.isFocused } }
        .flatten()
        .filter { it !is AutofillView.Unused }
        .takeUnless { it.isEmpty() }
        ?: viewsLists
            .flatten()
            .filter { it !is AutofillView.Unused })
        .map { it.updateWebsiteIfNecessary(website = urlBarWebsite) }
}

/**
 * This helper function updates the [ViewNodeTraversalData] if necessary for missing password
 * fields that were marked invalid because they contained a specific `hint` or `idEntry`. If the
 * current `ViewNodeTraversalData` contains at least one password fields, we do not add any fields.
 */
private fun ViewNodeTraversalData.updateForMissingPasswordFields(): ViewNodeTraversalData =
    if (this.autofillViews.none { it is AutofillView.Login.Password }) {
        this.copyAndMapAutofillViews { _, autofillView ->
            if (autofillView is AutofillView.Unused && autofillView.data.hasPasswordTerms) {
                AutofillView.Login.Password(data = autofillView.data)
            } else {
                autofillView
            }
        }
    } else {
        // We already have password fields available, so no need to add more.
        this
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
        this.copyAndMapAutofillViews { index, autofillView ->
            if (autofillView is AutofillView.Unused && passwordPositions.contains(index + 1)) {
                AutofillView.Login.Username(data = autofillView.data)
            } else {
                autofillView
            }
        }
    } else {
        // We already have username fields available or there are no password fields, so no need
        // to search for them.
        this
    }
}

/**
 * This helper function loops through all the [ViewNodeTraversalData.autofillViews] and returns the
 * fully updated `ViewNodeTraversalData`.
 */
private fun ViewNodeTraversalData.copyAndMapAutofillViews(
    mapper: (index: Int, autofillView: AutofillView) -> AutofillView,
): ViewNodeTraversalData {
    val updatedAutofillViews = autofillViews.mapIndexed(mapper)
    val previousUnusedIds = autofillViews
        .filterIsInstance<AutofillView.Unused>()
        .map { it.data.autofillId }
        .toSet()
    val currentUnusedIds = updatedAutofillViews
        .filterIsInstance<AutofillView.Unused>()
        .map { it.data.autofillId }
        .toSet()
    val unignoredAutofillIds = previousUnusedIds - currentUnusedIds
    return this.copy(
        autofillViews = updatedAutofillViews,
        ignoreAutofillIds = this.ignoreAutofillIds - unignoredAutofillIds,
    )
}

/**
 * Recursively traverse this [AssistStructure.ViewNode] and all of its descendants. Convert the
 * data into [ViewNodeTraversalData].
 *
 * [heuristicParentWebsite] is the direct parent's website, used only for the heuristic
 * [AutofillView] path (no cascading beyond one level). [fillAssistParentWebsite] cascades down the
 * tree so that web-view nodes with no explicit [AssistStructure.ViewNode.website] inherit their
 * ancestor's domain for fill-assist matching. [parentFillAssistHostRules] is the already-resolved
 * rules for the current host; re-resolved only when this node introduces a new website, avoiding
 * redundant map lookups for every node in the same web-view subtree.
 */
@Suppress("CyclomaticComplexMethod")
private fun AssistStructure.ViewNode.traverse(
    heuristicParentWebsite: String?,
    fillAssistParentWebsite: String?,
    parentFillAssistHostRules: List<FillAssistRules.HostRule>?,
    allFillAssistRules: Map<String, List<FillAssistRules.HostRule>>?,
): ViewNodeTraversalData {
    // Set up mutable lists for collecting valid AutofillViews and ignorable view ids.
    val mutableAutofillViewList: MutableList<AutofillView> = mutableListOf()
    val mutableFillAssistViewList: MutableList<AutofillView> = mutableListOf()
    val mutableIgnoreAutofillIdList: MutableList<AutofillId> = mutableListOf()
    // OS sometimes defaults node.idPackage to "android", which is not a valid
    // package name so it is ignored to prevent auto-filling unknown applications.
    var storedIdPackage: String? = this.idPackage?.takeUnless { it.isBlank() || it == "android" }
    val storedUrlBarId = storedIdPackage?.let { URL_BARS[it] }
    val storedUrlBarWebsites: MutableList<String> = this
        .website
        ?.takeIf { _ -> storedUrlBarId != null && storedUrlBarId == this.idEntry }
        ?.let { mutableListOf(it) }
        ?: mutableListOf()

    // For fill-assist, cascade the website so that child nodes without an explicit website
    // inherit the nearest ancestor's domain. Heuristic uses only the direct parent's website.
    val fillAssistWebsite = this.website ?: fillAssistParentWebsite

    // Only re-resolve host rules when this node introduces a new website (origin boundary).
    // Otherwise inherit the parent's already-resolved rules to avoid redundant lookups.
    val hostRules = if (this.website != null) {
        this.website
            ?.toUri()
            ?.host
            ?.removePrefix("www.")
            ?.let { host -> allFillAssistRules?.get(host) }
    } else {
        parentFillAssistHostRules
    }
    var storedFillAssistHostRules: List<FillAssistRules.HostRule>? = hostRules

    // Try converting this `ViewNode` into an `AutofillView`. If a valid instance is returned, add
    // it to the list. Otherwise, ignore the `AutofillId` associated with this `ViewNode`.
    toAutofillView(parentWebsite = heuristicParentWebsite)
        ?.run(mutableAutofillViewList::add)
        ?: autofillId?.run(mutableIgnoreAutofillIdList::add)

    // When fill-assist rules match this node, collect the resulting view.
    hostRules?.let { rules ->
        toFillAssistView(rules, fillAssistWebsite)?.run(mutableFillAssistViewList::add)
    }

    // Recursively traverse all of this view node's children.
    for (i in 0 until childCount) {
        // Extract the traversal data from each child view node and add it to the lists.
        getChildAt(i)
            .traverse(
                // Heuristic: pass this node's own website (no grandparent cascade).
                heuristicParentWebsite = website,
                // Fill-assist: pass the cascaded website so web-view subtrees stay associated
                // with their ancestor's domain.
                fillAssistParentWebsite = fillAssistWebsite,
                parentFillAssistHostRules = hostRules,
                allFillAssistRules = allFillAssistRules,
            )
            .let { viewNodeTraversalData ->
                viewNodeTraversalData.autofillViews.forEach(mutableAutofillViewList::add)
                viewNodeTraversalData.fillAssistViews.forEach(mutableFillAssistViewList::add)
                viewNodeTraversalData.ignoreAutofillIds.forEach(mutableIgnoreAutofillIdList::add)

                // Get the first non-null idPackage.
                if (storedIdPackage == null) {
                    storedIdPackage = viewNodeTraversalData.idPackage
                }
                // Bubble up the first fill-assist host rules encountered in the subtree so
                // parseInternal can use them without re-deriving from the URI.
                if (storedFillAssistHostRules == null) {
                    storedFillAssistHostRules = viewNodeTraversalData.fillAssistHostRules
                }
                // Add all url bar websites. We will deal with this later if
                // there is somehow more than one.
                storedUrlBarWebsites.addAll(viewNodeTraversalData.urlBarWebsites)
            }
    }

    // Build a new traversal data structure with this view node's data, and that of all of its
    // descendant's.
    return ViewNodeTraversalData(
        autofillViews = mutableAutofillViewList,
        fillAssistViews = mutableFillAssistViewList,
        fillAssistHostRules = storedFillAssistHostRules,
        idPackage = storedIdPackage,
        urlBarWebsites = storedUrlBarWebsites,
        ignoreAutofillIds = mutableIgnoreAutofillIdList,
    )
}

/**
 * This updates the underlying [AutofillView.data] with the given [website] if it does not already
 * have a website associated with it.
 */
private fun AutofillView.updateWebsiteIfNecessary(website: String?): AutofillView {
    val site = website ?: return this
    if (this.data.website != null) return this
    return when (this) {
        is AutofillView.Card.Brand -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.CardholderName -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.ExpirationDate -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.ExpirationMonth -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.ExpirationYear -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.Number -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Card.SecurityCode -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Login.Password -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Login.Username -> this.copy(data = this.data.copy(website = site))
        is AutofillView.Unused -> this.copy(data = this.data.copy(website = site))
    }
}
