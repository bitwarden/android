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
     * Fetches and persists fill-assist rules for the active server when the feature flag is
     * enabled and the cached data is stale. Returns [Result.failure] on network or parse failure.
     */
    suspend fun sync(): Result<Unit>

    /**
     * Returns the last successfully cached [FillAssistRules] for the active server, or null if
     * none exist.
     */
    fun getFillAssistRules(): FillAssistRules?
}
