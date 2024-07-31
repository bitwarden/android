package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.util.FakeServerConfigRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Instant

class FeatureFlagManagerTest {
    private val fakeDispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeConfigDiskSource = FakeConfigDiskSource()

    private val fakeServerConfigRepository = FakeServerConfigRepository(
        fakeConfigDiskSource,
    )

    private var manager = FeatureFlagManagerImpl(
        serverConfigRepository = fakeServerConfigRepository,
        configDiskSource = fakeConfigDiskSource,
        dispatcherManager = fakeDispatcherManager,
    )

    @Test
    fun `featureFlagsLocal should return set feature flags`() {
        val expected = mapOf("enableCipherKeyEncryption" to true)

        val actual = manager.featureFlagsLocal

        assertEquals(expected, actual)
    }

    @Test
    fun `ConfigDiskSource flow with value should trigger new flags`() = runTest {
        assertNull(
            fakeConfigDiskSource.serverConfig,
        )

        // This should trigger a new server config to be fetched
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG

        manager.featureFlagsServerStateFlow.test {
            assertNotNull(
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfigDiskSource flow with null should trigger null value`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG
        fakeConfigDiskSource.serverConfig = null

        manager.featureFlagsServerStateFlow.test {
            assertNull(
                awaitItem(),
            )
        }
    }

    @Test
    fun `getFeatureFlag Boolean should return value if exists`() = runTest {
        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = false,
            forceRefresh = true,
        )
        assertTrue(flagValue)
    }

    @Test
    fun `getFeatureFlag Boolean should return default value if doesn't exists`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("flag-example" to "123"),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = false,
            forceRefresh = false,
        )
        assertFalse(flagValue)
    }

    @Test
    fun `getFeatureFlag Int should return value if exists`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("email-verification" to "123"),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = 0,
            forceRefresh = false,
        )

        assertEquals(
            123,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Int should return default value if doesn't exists`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("flag-example" to "123"),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = 10,
            forceRefresh = false,
        )

        assertEquals(
            10,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return value if exists`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("email-verification" to "niceValue"),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = "",
            forceRefresh = false,
        )

        assertEquals(
            "niceValue",
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return default value if doesn't exists`() = runTest {
        fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
            serverData = SERVER_CONFIG
                .serverData
                .copy(
                    featureStates = mapOf("flag-example" to "niceValue"),
                ),
        )

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = "defaultValue",
            forceRefresh = false,
        )

        assertEquals(
            "defaultValue",
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Boolean should return default value if no flags available`() = runTest {
        fakeConfigDiskSource.serverConfig = null

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = true,
            forceRefresh = false,
        )

        assertTrue(
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag Int should return default value if no flags available`() = runTest {
        fakeConfigDiskSource.serverConfig = null

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = 10,
            forceRefresh = false,
        )

        assertEquals(
            10,
            flagValue,
        )
    }

    @Test
    fun `getFeatureFlag String should return default value if no flags available`() = runTest {
        fakeConfigDiskSource.serverConfig = null

        val flagValue = manager.getFeatureFlag(
            FlagKey.EmailVerification,
            defaultValue = "defaultValue",
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
            "email-verification" to "true",
            "flexible-collections-v-1" to "false",
        ),
    ),
)
