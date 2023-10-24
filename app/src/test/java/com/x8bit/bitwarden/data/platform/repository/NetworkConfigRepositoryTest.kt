package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
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

    private val authRepository: AuthRepository = mockk() {
        every { authStateFlow } returns mutableAuthStateFlow
    }

    private val authTokenInterceptor = AuthTokenInterceptor()

    private lateinit var networkConfigRepository: NetworkConfigRepository

    @BeforeEach
    fun setUp() {
        networkConfigRepository = NetworkConfigRepositoryImpl(
            authRepository = authRepository,
            authTokenInterceptor = authTokenInterceptor,
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
