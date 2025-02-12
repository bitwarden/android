package com.bitwarden.authenticator.data.platform.datasource.disk

import androidx.core.content.edit
import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.base.FakeSharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.datasource.network.di.PlatformNetworkModule
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class ConfigDiskSourceTest {
    private val json = PlatformNetworkModule.providesJson()

    private val fakeSharedPreferences = FakeSharedPreferences()

    private val configDiskSource = ConfigDiskSourceImpl(
        sharedPreferences = fakeSharedPreferences,
        json = json,
    )

    @Test
    fun `serverConfig should pull from and update SharedPreferences`() =
        runTest {
        val serverConfigKey = "bwPreferencesStorage:serverConfigurations"

        // Shared preferences and the repository start with the same value.
        assertNull(configDiskSource.serverConfig)
        assertNull(fakeSharedPreferences.getString(serverConfigKey, null))

        // Updating the repository updates shared preferences
        configDiskSource.serverConfig = SERVER_CONFIG
        assertEquals(
            json.parseToJsonElement(
                SERVER_CONFIG_JSON,
            ),
            json.parseToJsonElement(
                fakeSharedPreferences.getString(serverConfigKey, null)!!,
            ),
        )

        // Update SharedPreferences updates the repository
        fakeSharedPreferences.edit { putString(serverConfigKey, null) }
        assertNull(configDiskSource.serverConfig)
    }

    @Test
    fun `serverConfigFlow should react to changes in serverConfig`() =
        runTest {
            configDiskSource.serverConfigFlow.test {
                // The initial values of the Flow and the property are in sync
                assertNull(configDiskSource.serverConfig)
                assertNull(awaitItem())

                // Updating the repository updates shared preferences
                configDiskSource.serverConfig = SERVER_CONFIG
                assertEquals(SERVER_CONFIG, awaitItem())
            }
        }
}

private const val SERVER_CONFIG_JSON = """
{
  "lastSync": 1698408000000,
  "serverData": {
    "version": "2024.7.0",
    "gitHash": "25cf6119-dirty",
    "server": {
        "name": "example",
        "url": "https://localhost:8080"
    },
    "environment": {
        "vault": "https://localhost:8080",
        "api": "http://localhost:4000",
        "identity": "http://localhost:33656",
        "notifications": "http://localhost:61840",
        "sso": "http://localhost:51822"
    },
    "featureStates": {
        "duo-redirect": true,
        "flexible-collections-v-1": false
    }
  }
}

"""
private val SERVER_CONFIG = ServerConfig(
    lastSync = Instant.parse("2023-10-27T12:00:00Z").toEpochMilli(),
    serverData = ConfigResponseJson(
        type = null,
        version = "2024.7.0",
        gitHash = "25cf6119-dirty",
        server = ServerJson(
            name = "example",
            url = "https://localhost:8080",
        ),
        environment = EnvironmentJson(
            cloudRegion = null,
            vaultUrl = "https://localhost:8080",
            apiUrl = "http://localhost:4000",
            identityUrl = "http://localhost:33656",
            notificationsUrl = "http://localhost:61840",
            ssoUrl = "http://localhost:51822",
        ),
        featureStates = mapOf(
            "duo-redirect" to JsonPrimitive(true),
            "flexible-collections-v-1" to JsonPrimitive(false),
        ),
    ),
)
