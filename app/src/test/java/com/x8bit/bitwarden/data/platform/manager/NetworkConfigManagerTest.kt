package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.network.authenticator.RefreshAuthenticator
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkConfigManagerTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val mutableEnvironmentStateFlow = MutableStateFlow<Environment>(Environment.Us)

    private val authRepository: AuthRepository = mockk {
        every { authStateFlow } returns mutableAuthStateFlow
    }

    private val environmentRepository: EnvironmentRepository = mockk {
        every { environmentStateFlow } returns mutableEnvironmentStateFlow
    }

    private val refreshAuthenticator = RefreshAuthenticator()
    private val authTokenInterceptor = AuthTokenInterceptor()
    private val baseUrlInterceptors = BaseUrlInterceptors()

    private lateinit var networkConfigManager: NetworkConfigManager

    @BeforeEach
    fun setUp() {
        networkConfigManager = NetworkConfigManagerImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
            baseUrlInterceptors = baseUrlInterceptors,
            refreshAuthenticator = refreshAuthenticator,
            dispatcherManager = dispatcherManager,
        )
    }

    @Test
    fun `authenticatorProvider should be set on initialization`() {
        assertEquals(
            authRepository,
            refreshAuthenticator.authenticatorProvider,
        )
    }

    @Test
    fun `changes in the AuthState should update the AuthTokenInterceptor`() {
        mutableAuthStateFlow.value = AuthState.Uninitialized
        assertNull(authTokenInterceptor.authToken)

        mutableAuthStateFlow.value = AuthState.Authenticated(accessToken = "accessToken")
        assertEquals(
            "accessToken",
            authTokenInterceptor.authToken,
        )

        mutableAuthStateFlow.value = AuthState.Unauthenticated
        assertNull(authTokenInterceptor.authToken)
    }

    @Test
    fun `changes in the Environment should update the BaseUrlInterceptors`() {
        mutableEnvironmentStateFlow.value = Environment.Us
        assertEquals(
            Environment.Us,
            baseUrlInterceptors.environment,
        )

        mutableEnvironmentStateFlow.value = Environment.Eu
        assertEquals(
            Environment.Eu,
            baseUrlInterceptors.environment,
        )
    }
}
