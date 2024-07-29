package com.x8bit.bitwarden.data.platform.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.EnvironmentDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeConfigDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.EnvironmentJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.ConfigResponseJson.ServerJson
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.repository.util.toEnvironmentUrls
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnvironmentRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeConfigDiskSource = FakeConfigDiskSource()
    private val serverConfigRepository = ServerConfigRepositoryImpl(
        configService = mockk {
            coEvery { getConfig() } returns CONFIG_RESPONSE_JSON.asSuccess()
        },
        configDiskSource = fakeConfigDiskSource,
        dispatcherManager = dispatcherManager
    )

    private val fakeEnvironmentDiskSource = FakeEnvironmentDiskSource()
    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val repository = EnvironmentRepositoryImpl(
        environmentDiskSource = fakeEnvironmentDiskSource,
        serverConfigRepository = serverConfigRepository,
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = dispatcherManager,
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(EnvironmentUrlDataJson::toEnvironmentUrls)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(EnvironmentUrlDataJson::toEnvironmentUrls)
    }

    @Test
    fun `changes to the active user should update the environment if necessary`() {
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        assertEquals(
            null,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the environment for the active user to a non-null value triggers an update
        // in the saved environment.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = EnvironmentUrlDataJson.DEFAULT_EU,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the environment for the active user to a null value leaves the current
        // environment unchanged.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = null,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )
    }

    @Test
    fun `environment should pull from and update EnvironmentDiskSource`() {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        // The repository exposes a non-null default value when the disk source is empty
        assertNull(fakeEnvironmentDiskSource.preAuthEnvironmentUrlData)
        assertEquals(
            Environment.Us,
            repository.environment,
        )

        // Updating the repository updates the disk source
        repository.environment = environment
        assertEquals(
            environmentUrlDataJson,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )

        // Updating the disk source updates the repository
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = null
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        fakeEnvironmentDiskSource.preAuthEnvironmentUrlData = environmentUrlDataJson
        assertEquals(
            environment,
            repository.environment,
        )
    }

    @Test
    fun `environmentStateFow should react to changes in environment`() = runTest {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        repository.environmentStateFlow.test {
            // The initial values of the Flow and the property are in sync
            assertEquals(
                Environment.Us,
                repository.environment,
            )
            assertEquals(
                Environment.Us,
                awaitItem(),
            )

            // Updating the property causes a flow emissions
            repository.environment = environment
            assertEquals(environment, awaitItem())
        }
    }

    @Test
    fun `changes to the active user should fetch new server configurations`() {
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        assertEquals(
            null,
            fakeConfigDiskSource.serverConfig,
        )

        // Updating the environment for the active user to a non-null value triggers an update
        // in the saved environment.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = EnvironmentUrlDataJson.DEFAULT_EU,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )
        assertEquals(
            fakeConfigDiskSource.serverConfig!!.serverData,
            CONFIG_RESPONSE_JSON
        )

        // Updating the environment for the active user to a null value leaves the current
        // environment unchanged.
        fakeAuthDiskSource.userState = getMockUserState(
            environmentForActiveUser = null,
        )
        assertEquals(
            Environment.Eu,
            repository.environment,
        )
        assertEquals(
            EnvironmentUrlDataJson.DEFAULT_EU,
            fakeEnvironmentDiskSource.preAuthEnvironmentUrlData,
        )
        assertEquals(
            fakeConfigDiskSource.serverConfig!!.serverData,
            CONFIG_RESPONSE_JSON
        )
    }

    @Test
    fun `new server configurations should be fetched when environment changes`() = runTest {
        val environmentUrlDataJson = mockk<EnvironmentUrlDataJson>()
        val environment = mockk<Environment> {
            every { environmentUrlData } returns environmentUrlDataJson
        }
        every { environmentUrlDataJson.toEnvironmentUrls() } returns environment

        // The initial values
        assertEquals(
            Environment.Us,
            repository.environment,
        )
        assertEquals(
            fakeConfigDiskSource.serverConfig,
            null,
        )

        // Updating the property updates server configurations
        repository.environment = environment

        assertEquals(
            fakeConfigDiskSource.serverConfig!!.serverData,
            CONFIG_RESPONSE_JSON,
        )
    }

    private fun getMockUserState(
        environmentForActiveUser: EnvironmentUrlDataJson?,
    ): UserStateJson =
        UserStateJson(
            activeUserId = "activeUserId",
            accounts = mapOf(
                "activeUserId" to AccountJson(
                    profile = mockk(),
                    tokens = mockk(),
                    settings = AccountJson.Settings(
                        environmentUrlData = environmentForActiveUser,
                    ),
                ),
            ),
        )
}

private class FakeEnvironmentDiskSource : EnvironmentDiskSource {
    override var preAuthEnvironmentUrlData: EnvironmentUrlDataJson? = null
        set(value) {
            field = value
            mutablePreAuthEnvironmentUrlDataFlow.tryEmit(value)
        }

    override val preAuthEnvironmentUrlDataFlow: Flow<EnvironmentUrlDataJson?>
        get() = mutablePreAuthEnvironmentUrlDataFlow
            .onSubscription { emit(preAuthEnvironmentUrlData) }

    private val mutablePreAuthEnvironmentUrlDataFlow =
        bufferedMutableSharedFlow<EnvironmentUrlDataJson?>(replay = 1)
}

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
    featureStates = mapOf("duo-redirect" to true, "flexible-collections-v-1" to false)
)