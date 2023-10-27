package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.BaseUrlInterceptors
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConfigManagerTest {
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val mutableEnvironmentStateFlow = MutableStateFlow<Environment>(Environment.Us)

    private val authRepository: AuthRepository = mockk() {
        every { authStateFlow } returns mutableAuthStateFlow
    }

    private val environmentRepository: EnvironmentRepository = mockk {
        every { environmentStateFlow } returns mutableEnvironmentStateFlow
    }

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
            dispatcher = UnconfinedTestDispatcher(),
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
