package com.x8bit.bitwarden.data.auth.repository

import com.bitwarden.core.Kdf
import com.bitwarden.sdk.Client
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryTest {

    private val accountsService: AccountsService = mockk()
    private val identityService: IdentityService = mockk()
    private val authInterceptor = mockk<AuthTokenInterceptor>()
    private val mockBitwardenSdk = mockk<Client> {
        coEvery {
            auth().hashPassword(
                email = EMAIL,
                password = PASSWORD,
                kdfParams = Kdf.Pbkdf2(iterations = PRE_LOGIN_SUCCESS.kdfIterations),
            )
        } returns PASSWORD_HASH
    }

    private val repository = AuthRepositoryImpl(
        accountsService = accountsService,
        identityService = identityService,
        bitwardenSdkClient = mockBitwardenSdk,
        authTokenInterceptor = authInterceptor,
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(identityService, accountsService, authInterceptor)
    }

    @Test
    fun `login when pre login fails should return Error`() = runTest {
        coEvery { accountsService.preLogin(EMAIL) } returns (Result.failure(RuntimeException()))
        val result = repository.login(EMAIL, PASSWORD)
        assertEquals(LoginResult.Error, result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(EMAIL) }
    }

    @Test
    fun `login get token fails should return Error`() = runTest {
        coEvery { accountsService.preLogin(EMAIL) } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery { identityService.getToken(EMAIL, PASSWORD_HASH) }
            .returns(Result.failure(RuntimeException()))
        val result = repository.login(EMAIL, PASSWORD)
        assertEquals(LoginResult.Error, result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(EMAIL) }
        coVerify { identityService.getToken(EMAIL, PASSWORD_HASH) }
    }

    @Test
    fun `login get token succeeds should return Success and update AuthState`() = runTest {
        coEvery { accountsService.preLogin(EMAIL) } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery { identityService.getToken(email = EMAIL, passwordHash = PASSWORD_HASH) }
            .returns(Result.success(GetTokenResponseJson.Success(accessToken = ACCESS_TOKEN)))
        every { authInterceptor.authToken = ACCESS_TOKEN } returns Unit
        val result = repository.login(EMAIL, PASSWORD)
        assertEquals(LoginResult.Success, result)
        assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
        verify { authInterceptor.authToken = ACCESS_TOKEN }
        coVerify { accountsService.preLogin(EMAIL) }
        coVerify { identityService.getToken(EMAIL, PASSWORD_HASH) }
    }

    @Test
    fun `login get token returns captcha request should return CaptchaRequired`() = runTest {
        coEvery { accountsService.preLogin(EMAIL) } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery { identityService.getToken(email = EMAIL, passwordHash = PASSWORD_HASH) }
            .returns(Result.success(GetTokenResponseJson.CaptchaRequired(CAPTCHA_KEY)))
        val result = repository.login(EMAIL, PASSWORD)
        assertEquals(LoginResult.CaptchaRequired(CAPTCHA_KEY), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(EMAIL) }
        coVerify { identityService.getToken(EMAIL, PASSWORD_HASH) }
    }

    companion object {
        private const val EMAIL = "test@test.com"
        private const val PASSWORD = "password"
        private const val PASSWORD_HASH = "passwordHash"
        private const val ACCESS_TOKEN = "accessToken"
        private const val CAPTCHA_KEY = "captcha"
        private val PRE_LOGIN_SUCCESS = PreLoginResponseJson(
            kdf = 1,
            kdfIterations = 1u,
            kdfMemory = null,
            kdfParallelism = null,
        )
    }
}
