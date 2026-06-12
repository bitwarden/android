package com.x8bit.bitwarden.data.autofill.datasource.disk

import com.bitwarden.data.datasource.disk.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.autofill.model.FillAssistRules
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FillAssistDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val diskSource = FillAssistDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `migration clears all fill-assist data across all servers`() {
        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_1, rules = FILL_ASSIST_RULES)
        diskSource.storeLastKnownCid(serverUrl = SERVER_URL_1, cid = "sha256:abc")
        diskSource.storeLastFetchTimestamp(serverUrl = SERVER_URL_1, timestamp = 123L)
        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_2, rules = FILL_ASSIST_RULES)

        // Trigger migration by writing a stale version — clears data for all servers.
        fakeSharedPreferences.edit()
            .putInt("bwPreferencesStorage:fillAssistCacheVersion", -1)
            .apply()
        val clearedDiskSource = FillAssistDiskSourceImpl(
            sharedPreferences = fakeSharedPreferences,
            json = json,
        )

        assertNull(clearedDiskSource.getFillAssistRules(serverUrl = SERVER_URL_1))
        assertNull(clearedDiskSource.getLastKnownCid(serverUrl = SERVER_URL_1))
        assertNull(clearedDiskSource.getLastFetchTimestamp(serverUrl = SERVER_URL_1))
        assertNull(clearedDiskSource.getFillAssistRules(serverUrl = SERVER_URL_2))
    }

    @Test
    fun `storeFillAssistRules and getFillAssistRules round-trip`() {
        assertNull(diskSource.getFillAssistRules(serverUrl = SERVER_URL_1))

        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_1, rules = FILL_ASSIST_RULES)
        assertEquals(FILL_ASSIST_RULES, diskSource.getFillAssistRules(serverUrl = SERVER_URL_1))

        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_1, rules = null)
        assertNull(diskSource.getFillAssistRules(serverUrl = SERVER_URL_1))
    }

    @Test
    fun `data is scoped per server, one server does not affect another`() {
        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_1, rules = FILL_ASSIST_RULES)

        assertNull(diskSource.getFillAssistRules(serverUrl = SERVER_URL_2))
        assertEquals(FILL_ASSIST_RULES, diskSource.getFillAssistRules(serverUrl = SERVER_URL_1))
    }

    @Test
    fun `storeLastKnownCid and getLastKnownCid round-trip`() {
        val cid = "sha256:5b8f688d24bb9c38b4094838fa2baacb3cc4ab302e3545adf016b05f6b6b96db"
        assertNull(diskSource.getLastKnownCid(serverUrl = SERVER_URL_1))

        diskSource.storeLastKnownCid(serverUrl = SERVER_URL_1, cid = cid)
        assertEquals(cid, diskSource.getLastKnownCid(serverUrl = SERVER_URL_1))

        diskSource.storeLastKnownCid(serverUrl = SERVER_URL_1, cid = null)
        assertNull(diskSource.getLastKnownCid(serverUrl = SERVER_URL_1))
    }

    @Test
    fun `storeLastFetchTimestamp and getLastFetchTimestamp round-trip`() {
        val timestamp = 1716307262956L
        assertNull(diskSource.getLastFetchTimestamp(serverUrl = SERVER_URL_1))

        diskSource.storeLastFetchTimestamp(serverUrl = SERVER_URL_1, timestamp = timestamp)
        assertEquals(timestamp, diskSource.getLastFetchTimestamp(serverUrl = SERVER_URL_1))

        diskSource.storeLastFetchTimestamp(serverUrl = SERVER_URL_1, timestamp = null)
        assertNull(diskSource.getLastFetchTimestamp(serverUrl = SERVER_URL_1))
    }

    @Test
    fun `migration does not clear fillAssistRulesUrl from EnvironmentDiskSource`() {
        fakeSharedPreferences.edit()
            .putString(
                "bwPreferencesStorage:fillAssistRulesUrl",
                "https://fill-assist.example.com/",
            )
            .putInt("bwPreferencesStorage:fillAssistCacheVersion", -1)
            .apply()

        FillAssistDiskSourceImpl(sharedPreferences = fakeSharedPreferences, json = json)

        assertEquals(
            "https://fill-assist.example.com/",
            fakeSharedPreferences.getString(
                key = "bwPreferencesStorage:fillAssistRulesUrl",
                defaultValue = null,
            ),
        )
    }

    @Test
    fun `migration preserves data when cache version is current`() {
        diskSource.storeFillAssistRules(serverUrl = SERVER_URL_1, rules = FILL_ASSIST_RULES)
        diskSource.storeLastKnownCid(serverUrl = SERVER_URL_1, cid = "sha256:abc")

        // New instance with the same preferences — version already set to current by first init.
        val sameDiskSource = FillAssistDiskSourceImpl(
            sharedPreferences = fakeSharedPreferences,
            json = json,
        )

        assertEquals(FILL_ASSIST_RULES, sameDiskSource.getFillAssistRules(serverUrl = SERVER_URL_1))
        assertEquals("sha256:abc", sameDiskSource.getLastKnownCid(serverUrl = SERVER_URL_1))
    }
}

private const val SERVER_URL_1 = "https://fill-assist.example.com"
private const val SERVER_URL_2 = "https://fill-assist.other.com"

private val FILL_ASSIST_RULES = FillAssistRules(
    hostRules = mapOf(
        "example.com" to listOf(
            FillAssistRules.HostRule(
                category = "account-login",
                fields = mapOf(
                    "username" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = "email",
                            name = null,
                            type = null,
                            role = null,
                        ),
                    ),
                    "password" to listOf(
                        FillAssistRules.SelectorClause(
                            tag = "input",
                            id = null,
                            name = "pass",
                            type = "password",
                            role = null,
                        ),
                    ),
                ),
            ),
        ),
    ),
)
