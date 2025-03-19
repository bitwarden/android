package com.bitwarden.authenticator.data.platform.repository

import app.cash.turbine.test
import com.bitwarden.authenticator.data.platform.base.FakeDispatcherManager
import com.bitwarden.authenticator.data.platform.datasource.disk.model.ServerConfig
import com.bitwarden.authenticator.data.platform.datasource.disk.util.FakeConfigDiskSource
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.bitwarden.authenticator.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.bitwarden.authenticator.data.platform.datasource.network.service.ConfigService
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ServerConfigRepositoryTest {
    private val fakeDispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeConfigDiskSource = FakeConfigDiskSource()
    private val configService: ConfigService = mockk {
        coEvery {
            getConfig()
        } returns CONFIG_RESPONSE_JSON.asSuccess()
    }

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val repository = ServerConfigRepositoryImpl(
        configDiskSource = fakeConfigDiskSource,
        configService = configService,
        clock = fixedClock,
        dispatcherManager = fakeDispatcherManager,
    )

    @BeforeEach
    fun setUp() {
        fakeConfigDiskSource.serverConfig = null
    }

    @Test
    fun `getServerConfig should fetch a new server configuration with force refresh as true`() =
        runTest {
            coEvery {
                configService.getConfig()
            } returns CONFIG_RESPONSE_JSON.copy(version = "NEW VERSION").asSuccess()

            fakeConfigDiskSource.serverConfig = SERVER_CONFIG.copy(
                lastSync = fixedClock.instant().toEpochMilli(),
            )

            assertEquals(
                fakeConfigDiskSource.serverConfig,
                SERVER_CONFIG,
            )

            repository.getServerConfig(forceRefresh = true)

            assertNotEquals(
                fakeConfigDiskSource.serverConfig,
                SERVER_CONFIG,
            )
        }

    @Test
    fun `getServerConfig should fetch a new server configuration if there is none in state`() =
        runTest {
            assertNull(
                fakeConfigDiskSource.serverConfig,
            )

            repository.getServerConfig(forceRefresh = false)

            assertEquals(
                fakeConfigDiskSource.serverConfig,
                SERVER_CONFIG,
            )
        }

    @Test
    fun `getServerConfig should return state server config if refresh is not necessary`() =
        runTest {
            val testConfig = SERVER_CONFIG.copy(
                lastSync = fixedClock.instant().plusSeconds(1000L).toEpochMilli(),
                serverData = CONFIG_RESPONSE_JSON.copy(
                    version = "new version!!",
                ),
            )
            fakeConfigDiskSource.serverConfig = testConfig

            coEvery {
                configService.getConfig()
            } returns CONFIG_RESPONSE_JSON.asSuccess()

            repository.getServerConfig(forceRefresh = false)

            assertEquals(
                fakeConfigDiskSource.serverConfig,
                testConfig,
            )
        }

    @Test
    fun `serverConfigStateFlow should react to new server configurations`() = runTest {
        repository.getServerConfig(forceRefresh = true)

        repository.serverConfigStateFlow.test {
            assertEquals(fakeConfigDiskSource.serverConfig, awaitItem())
        }
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
            "duo-redirect" to JsonPrimitive(true),
            "flexible-collections-v-1" to JsonPrimitive(false),
        ),
    ),
)

private val CONFIG_RESPONSE_JSON = ConfigResponseJson(
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
)
