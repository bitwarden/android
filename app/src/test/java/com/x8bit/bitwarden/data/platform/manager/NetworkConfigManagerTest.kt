package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.ServerConfigRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.util.advanceTimeByAndRunCurrent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConfigManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager(
        unconfined = testDispatcher,
    )
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val mutableEnvironmentStateFlow = MutableStateFlow<Environment>(Environment.Us)

    private val authRepository: AuthRepository = mockk {
        every { authStateFlow } returns mutableAuthStateFlow
    }
    private val environmentRepository: EnvironmentRepository = mockk {
        every { environmentStateFlow } returns mutableEnvironmentStateFlow
    }
    private val serverConfigRepository: ServerConfigRepository = mockk {
        coEvery { getServerConfig(forceRefresh = true) } returns null
    }

    private val refreshAuthenticator = RefreshAuthenticator()

    private lateinit var networkConfigManager: NetworkConfigManager

    @BeforeEach
    fun setUp() {
        networkConfigManager = NetworkConfigManagerImpl(
            authRepository = authRepository,
            environmentRepository = environmentRepository,
            serverConfigRepository = serverConfigRepository,
            refreshAuthenticator = refreshAuthenticator,
            dispatcherManager = dispatcherManager,
        )
    }

    @Test
    fun `changes in the Environment should call getServerConfig after debounce period`() {
        mutableEnvironmentStateFlow.value = Environment.Us
        mutableEnvironmentStateFlow.value = Environment.Eu
        testDispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 500L)
        coVerify(exactly = 1) {
            serverConfigRepository.getServerConfig(forceRefresh = true)
        }
    }
}
