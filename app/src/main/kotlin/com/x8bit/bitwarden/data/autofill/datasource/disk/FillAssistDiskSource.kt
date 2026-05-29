package com.x8bit.bitwarden.data.autofill.datasource.disk

import com.x8bit.bitwarden.data.autofill.model.FillAssistRules

/**
 * Disk source for persisting fill-assist targeting rules per server.
 *
 * All operations are scoped by [serverUrl] (the fill-assist CDN base URL provided by the server
 * config), so multiple accounts on the same server share one cached copy of the rules while
 * accounts on different servers remain independent.
 */
interface FillAssistDiskSource {

    /**
     * Returns the cached [FillAssistRules] for [serverUrl], or null if none are stored.
     */
    fun getFillAssistRules(serverUrl: String): FillAssistRules?

    /**
     * Stores [rules] for [serverUrl], or removes the entry when [rules] is null.
     */
    fun storeFillAssistRules(serverUrl: String, rules: FillAssistRules?)

    /**
     * Returns the last known content hash (CID) for [serverUrl], or null if none is stored.
     */
    fun getLastKnownCid(serverUrl: String): String?

    /**
     * Stores [cid] for [serverUrl], or removes the entry when [cid] is null.
     */
    fun storeLastKnownCid(serverUrl: String, cid: String?)

    /**
     * Returns the epoch-millisecond timestamp of the last successful fetch for [serverUrl],
     * or null if never fetched.
     */
    fun getLastFetchTimestamp(serverUrl: String): Long?

    /**
     * Stores [timestamp] for [serverUrl], or removes the entry when [timestamp] is null.
     */
    fun storeLastFetchTimestamp(serverUrl: String, timestamp: Long?)
}
