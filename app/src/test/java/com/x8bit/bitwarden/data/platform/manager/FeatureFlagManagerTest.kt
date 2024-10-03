package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.FakeServerConfigRepository
import com.x8bit.bitwarden.data.platform.util.isServerVersionAtLeast
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant

class FeatureFlagManagerTest {
    private val fakeServerConfigRepository = FakeServerConfigRepository()

    private var manager = FeatureFlagManagerImpl(
        serverConfigRepository = fakeServerConfigRepository,
    )

    @Test
    fun `sdkFeatureFlags should return set feature flags`() {
        val expected = mapOf("enableCipherKeyEncryption" to true)

        val actual = manager.sdkFeatureFlags

        assertEquals(expected, actual)
    }

    @Test
    fun `server version is at least supplied version`() {
        val result =
            isServerVersionAtLeast(
                fakeServerConfigRepository.serverConfigStateFlow.value,
                "2024.2.0",
            )

        // This relies on the fake server version being "2024.7.0"
        assertTrue(result)
    }

    @Test
    fun `server version is not at least supplied version`() {
        val result =
            isServerVersionAtLeast(
                fakeServerConfigRepository.serverConfigStateFlow.value,
                "2024.12.0-suffix",
            )

        // This relies on the fake server version being "2024.7.0"
        assertFalse(result)
    }

    @Test
    fun `server version is the same as supplied version`() {
        val result =
            isServerVersionAtLeast(
                fakeServerConfigRepository.serverConfigStateFlow.value,
                "2024.7.0",
            )

        // This relies on the fake server version being "2024.7.0"
        assertTrue(result)
    }

    @Test
    fun `server version is not the same as blank supplied version`() {
        val result =
            isServerVersionAtLeast(
                fakeServerConfigRepository.serverConfigStateFlow.value,
                "",
            )

        assertFalse(result)
    }

    @Test
    fun `ServerConfigRepository flow with value should trigger new flags`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null
        assertNull(
            fakeServerConfigRepository.serverConfigValue,
        )

        // This should trigger a new server config to be fetched
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG

        manager.getFeatureFlagFlow(FlagKey.DummyBoolean).test {
            assertNotNull(
                awaitItem(),
            )
        }
    }

    @Test
    fun `ServerConfigRepository flow with null should trigger default flag value value`() =
        runTest {
            fakeServerConfigRepository.serverConfigValue = null

            manager.getFeatureFlagFlow(FlagKey.DummyBoolean).test {
                assertFalse(
                    awaitItem(),
                )
            }
        }

    @Test
    fun `getFeatureFlag Boolean should return value if exists`() = runTest {
        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyBoolean,
            forceRefresh = true,
        )
        assertTrue(flagValue)
    }

    @Test
    fun `getFeatureFlag Boolean should return default value if doesn't exists`() = runTest {
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("flag-example" to JsonPrimitive(123)),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.EmailVerification,
            forceRefresh = false,
        )
        assertFalse(flagValue)
    }

    @Test
    fun `getFeatureFlag Int should return value if exists`() = runTest {
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("dummy-int" to JsonPrimitive(123)),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyInt(),
            forceRefresh = false,
        )

        assertEquals(
            123,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Int should return default value if doesn't exists`() = runTest {
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("flag-example" to JsonPrimitive(123)),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyInt(),
            forceRefresh = false,
        )

        assertEquals(
            Int.MIN_VALUE,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return value if exists`() = runTest {
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("dummy-string" to JsonPrimitive("niceValue")),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyString,
            forceRefresh = false,
        )

        assertEquals(
            "niceValue",
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return default value if doesn't exists`() =
        runTest {
            fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
                serverData = SERVER_CONFIG
                    .serverData
                    .copy(
                        featureStates = mapOf("flag-example" to JsonPrimitive("niceValue")),
                    ),
            )

            val flagValue = manager.getFeatureFlag(
                key = FlagKey.DummyString,
                forceRefresh = false,
            )

            assertEquals(
                "defaultValue",
                flagValue,
            )
        }

    @Test
    fun `getFeatureFlag Boolean should return default value if no flags available`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.EmailVerification,
            forceRefresh = false,
        )

        assertFalse(
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Int should return default value if no flags available`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyInt(),
            forceRefresh = false,
        )

        assertEquals(
            Int.MIN_VALUE,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Int should return default value when not remotely controlled`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyInt(isRemotelyConfigured = false),
            forceRefresh = false,
        )

        assertEquals(
            Int.MIN_VALUE,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return default value if no flags available`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyString,
            forceRefresh = false,
        )

        assertEquals(
            "defaultValue",
            flagValue,
        )
    }

    @Test
    fun `synchronous getFeatureFlag should return stored value when present`() {
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG.serverData.copy(
                featureStates = mapOf("dummy-int" to JsonPrimitive(true)),
            ),
        )

        val flagValue = manager.getFeatureFlag(key = FlagKey.DummyInt())

        assertEquals(Int.MIN_VALUE, flagValue)
    }

    @Test
    fun `synchronous getFeatureFlag should return default value if flag is incorrect type`() {
        val value = "nonDefaultValue"
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG.serverData.copy(
                featureStates = mapOf("dummy-string" to JsonPrimitive(value)),
            ),
        )

        val flagValue = manager.getFeatureFlag(key = FlagKey.DummyString)

        assertEquals(value, flagValue)
    }

    @Test
    fun `synchronous getFeatureFlag should return default value if no flags available`() {
        fakeServerConfigRepository.serverConfigValue = null

        val flagValue = manager.getFeatureFlag(key = FlagKey.DummyString)

        assertEquals("defaultValue", flagValue)
    }
}

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
            "dummy-boolean" to JsonPrimitive(true),
            "flexible-collections-v-1" to JsonPrimitive(false),
        ),
    ),
)
