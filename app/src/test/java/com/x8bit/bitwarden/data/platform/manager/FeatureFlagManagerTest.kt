package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.x8bit.bitwarden.data.platform.repository.util.FakeServerConfigRepository
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
    fun `ServerConfigRepository flow with value should trigger new flags`() = runTest {
        fakeServerConfigRepository.serverConfigValue = null
        assertNull(
            fakeServerConfigRepository.serverConfigValue,
        )

        // This should trigger a new server config to be fetched
        fakeServerConfigRepository.serverConfigValue = SERVER_CONFIG

        manager.getFeatureFlagFlow(FlagKey.EmailVerification).test {
            assertNotNull(
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfigDiskSource flow with null should trigger default flag value value`() = runTest {
        fakeServerConfigRepository.mutableServerConfigFlow.tryEmit(null)

        manager.getFeatureFlagFlow(FlagKey.EmailVerification).test {
            assertFalse(
                awaitItem(),
            )
        }
    }

    @Test
    fun `getFeatureFlag Boolean should return value if exists`() = runTest {
        val flagValue = manager.getFeatureFlag(
            key = FlagKey.EmailVerification,
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
                    featureStates = mapOf("email-verification" to JsonPrimitive(123)),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            key = FlagKey.DummyInt,
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
            key = FlagKey.DummyInt,
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
                    featureStates = mapOf("email-verification" to JsonPrimitive("niceValue")),
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
    fun `getFeatureFlag String should return default value if doesn't exists`() = runTest {
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
            key = FlagKey.DummyInt,
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
            "email-verification" to JsonPrimitive(true),
            "flexible-collections-v-1" to JsonPrimitive(false),
        ),
    ),
)
