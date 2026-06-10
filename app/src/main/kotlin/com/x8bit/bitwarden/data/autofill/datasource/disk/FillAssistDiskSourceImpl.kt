package com.x8bit.bitwarden.data.autofill.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.data.datasource.disk.BaseDiskSource
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import kotlinx.serialization.json.Json

// Bump this constant in two cases:
// 1. The parsing logic changes in a way that invalidates previously cached results.
// 2. EXPECTED_SCHEMA_MAJOR in FillAssistManagerImpl is updated to support a new schema major.
//    Without bumping this, the staleness check would skip re-downloading data that was previously
//    rejected for an unsupported schema — meaning the app would never pick up the new rules.
// On the next app launch after a bump, all stored fill-assist data is cleared and re-downloaded.
private const val CURRENT_CACHE_VERSION = 0

private const val FILL_ASSIST_CACHE_VERSION_KEY = "fillAssistCacheVersion"
private const val FILL_ASSIST_RULES_KEY = "fillAssistRules"
private const val FILL_ASSIST_CID_KEY = "fillAssistLastCid"
private const val FILL_ASSIST_TIMESTAMP_KEY = "fillAssistLastFetchTimestamp"

/**
 * Primary implementation of [FillAssistDiskSource].
 */
class FillAssistDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences),
    FillAssistDiskSource {

    init {
        performMigrationIfNeeded()
    }

    override fun getFillAssistRules(serverUrl: String): FillAssistRules? =
        getString(FILL_ASSIST_RULES_KEY.appendIdentifier(serverUrl))
            ?.let { json.decodeFromStringOrNull(it) }

    override fun storeFillAssistRules(serverUrl: String, rules: FillAssistRules?) {
        putString(
            FILL_ASSIST_RULES_KEY.appendIdentifier(serverUrl),
            rules?.let { json.encodeToString(it) },
        )
    }

    override fun getLastKnownCid(serverUrl: String): String? =
        getString(FILL_ASSIST_CID_KEY.appendIdentifier(serverUrl))

    override fun storeLastKnownCid(serverUrl: String, cid: String?) {
        putString(FILL_ASSIST_CID_KEY.appendIdentifier(serverUrl), cid)
    }

    override fun getLastFetchTimestamp(serverUrl: String): Long? =
        getLong(FILL_ASSIST_TIMESTAMP_KEY.appendIdentifier(serverUrl))

    override fun storeLastFetchTimestamp(serverUrl: String, timestamp: Long?) {
        putLong(FILL_ASSIST_TIMESTAMP_KEY.appendIdentifier(serverUrl), timestamp)
    }

    private fun performMigrationIfNeeded() {
        if (getInt(FILL_ASSIST_CACHE_VERSION_KEY) == CURRENT_CACHE_VERSION) return
        clearAllData()
    }

    private fun clearAllData() {
        removeWithPrefix("${FILL_ASSIST_RULES_KEY}_")
        removeWithPrefix("${FILL_ASSIST_CID_KEY}_")
        removeWithPrefix("${FILL_ASSIST_TIMESTAMP_KEY}_")
        putInt(FILL_ASSIST_CACHE_VERSION_KEY, CURRENT_CACHE_VERSION)
    }
}
