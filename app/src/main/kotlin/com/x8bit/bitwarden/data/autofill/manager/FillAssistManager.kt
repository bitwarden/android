package com.x8bit.bitwarden.data.autofill.manager

import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

/**
 * Manages fetching and caching fill-assist targeting rules.
 *
 * Rules are scoped per server (the fill-assist CDN URL from server config), so multiple accounts
 * on the same server share one cached copy.
 */
interface FillAssistManager {
    /**
     * Triggers a background sync if no sync is currently running. The sync fetches and persists
     * fill-assist rules when the feature flag is enabled and cached data is stale.
     */
    fun syncIfNecessary()

    /**
     * Returns the last successfully cached [FillAssistRules] for the active server, or null if
     * none exist.
     */
    fun getFillAssistRules(): FillAssistRules?
}
