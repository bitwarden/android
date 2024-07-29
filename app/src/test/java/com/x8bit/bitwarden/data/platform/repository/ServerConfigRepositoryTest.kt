package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.model.ServerConfig
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.x8bit.bitwarden.data.platform.datasource.network.service.ConfigService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant

class ServerConfigRepositoryTest {
    private val fakeDispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeConfigDiskSource = FakeConfigDiskSource()
    private val configService: ConfigService = mockk()

    private val repository = ServerConfigRepositoryImpl(
        configDiskSource = fakeConfigDiskSource,
        configService = configService,
        dispatcherManager = fakeDispatcherManager,
    )

    @Test
    fun `getServerConfig should fetch a new server configuration with force refresh as true`() =
        runTest {
            coEvery {
                configService.getConfig()
            } returns CONFIG_RESPONSE_JSON.copy(version = "NEW VERSION").asSuccess()

            val testConfig = SERVER_CONFIG.copy(
                lastSync = Instant.now().toEpochMilli(),
            )
            fakeConfigDiskSource.serverConfig = testConfig

            assertNotNull(
                fakeConfigDiskSource.serverConfig,
            )
            assertEquals(
                fakeConfigDiskSource.serverConfig!!.serverData,
                SERVER_CONFIG.serverData,
            )

            repository.getServerConfig(forceRefresh = true)

            assertNotEquals(
                fakeConfigDiskSource.serverConfig!!.serverData,
                SERVER_CONFIG.serverData,
            )
        }

    @Test
    fun `getServerConfig should fetch a new server configuration if there is none in state`() =
        runTest {
            coEvery {
                configService.getConfig()
            } returns CONFIG_RESPONSE_JSON.asSuccess()

            assertEquals(
                null,
                fakeConfigDiskSource.serverConfig,
            )

            repository.getServerConfig(forceRefresh = false)

            assertNotNull(
                fakeConfigDiskSource.serverConfig,
            )

            assertEquals(
                fakeConfigDiskSource.serverConfig!!.serverData,
                SERVER_CONFIG.serverData,
            )
        }

    @Test
    fun `getServerConfig should return state server config if refresh is not necessary`() =
        runTest {
            val testConfig = SERVER_CONFIG.copy(
                lastSync = Instant.now().plusSeconds(1000L).toEpochMilli(),
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
        featureStates = mapOf("duo-redirect" to true, "flexible-collections-v-1" to false)
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
    featureStates = mapOf("duo-redirect" to true, "flexible-collections-v-1" to false),
)
