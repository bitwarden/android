package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
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
class NetworkConfigRepositoryTest {
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    private val mutableEnvironmentStateFlow = MutableStateFlow<Environment>(Environment.Us)

    private val authRepository: AuthRepository = mockk() {
        every { authStateFlow } returns mutableAuthStateFlow
    }

    private val environmentRepository: EnvironmentRepository = mockk {
        every { environmentStateFlow } returns mutableEnvironmentStateFlow
    }

    private val authTokenInterceptor = AuthTokenInterceptor()

    private lateinit var networkConfigRepository: NetworkConfigRepository

    @BeforeEach
    fun setUp() {
        networkConfigRepository = NetworkConfigRepositoryImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
            environmentRepository = environmentRepository,
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
}
