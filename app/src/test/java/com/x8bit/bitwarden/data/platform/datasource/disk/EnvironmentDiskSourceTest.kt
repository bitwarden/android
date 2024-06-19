package com.x8bit.bitwarden.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EnvironmentDiskSourceTest {
    private val fakeSharedPreferences = FakeSharedPreferences()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val environmentDiskSource = EnvironmentDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `preAuthEnvironmentUrlData should pull from and update SharedPreferences`() {
        val environmentKey = "bwPreferencesStorage:preAuthEnvironmentUrls"

        // Shared preferences and the repository start with the same value.
        assertNull(environmentDiskSource.preAuthEnvironmentUrlData)
        assertNull(fakeSharedPreferences.getString(environmentKey, null))

        // Updating the repository updates shared preferences
        environmentDiskSource.preAuthEnvironmentUrlData = ENVIRONMENT_URL_DATA
        assertEquals(
            json.parseToJsonElement(
                ENVIRONMENT_URL_DATA_JSON,
            ),
            json.parseToJsonElement(
                fakeSharedPreferences.getString(environmentKey, null)!!,
            ),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit { putString(environmentKey, null) }
        assertNull(environmentDiskSource.preAuthEnvironmentUrlData)
    }

    @Test
    fun `preAuthEnvironmentUrlDataFlow should react to changes in preAuthEnvironmentUrlData`() =
        runTest {
            environmentDiskSource.preAuthEnvironmentUrlDataFlow.test {
                // The initial values of the Flow and the property are in sync
                assertNull(environmentDiskSource.preAuthEnvironmentUrlData)
                assertNull(awaitItem())

                // Updating the repository updates shared preferences
                environmentDiskSource.preAuthEnvironmentUrlData = ENVIRONMENT_URL_DATA
                assertEquals(ENVIRONMENT_URL_DATA, awaitItem())
            }
        }
}

private const val ENVIRONMENT_URL_DATA_JSON = """
    {
      "base": "base",
      "api": "api",
      "identity": "identity",
      "icons": "icon",
      "notifications": "notifications",
      "webVault": "webVault",
      "events": "events"
    }
"""

private val ENVIRONMENT_URL_DATA = EnvironmentUrlDataJson(
    base = "base",
    api = "api",
    identity = "identity",
    icon = "icon",
    notifications = "notifications",
    webVault = "webVault",
    events = "events",
)
