package com.x8bit.bitwarden.data.auth.repository

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthState
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.service.AccountsService
import com.x8bit.bitwarden.data.auth.datasource.network.service.IdentityService
import com.x8bit.bitwarden.data.auth.datasource.network.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.datasource.sdk.AuthSdkSource
import com.x8bit.bitwarden.data.auth.util.toSdkParams
import com.x8bit.bitwarden.data.platform.datasource.network.interceptor.AuthTokenInterceptor
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryTest {

    private val accountsService: AccountsService = mockk()
    private val identityService: IdentityService = mockk()
    private val authInterceptor = mockk<AuthTokenInterceptor>()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val authSdkSource = mockk<AuthSdkSource> {
        coEvery {
            hashPassword(
                email = EMAIL,
                password = PASSWORD,
                kdf = PRE_LOGIN_SUCCESS.kdfParams.toSdkParams(),
            )
        } returns Result.success(PASSWORD_HASH)
    }

    private val repository = AuthRepositoryImpl(
        accountsService = accountsService,
        identityService = identityService,
        authSdkSource = authSdkSource,
        authDiskSource = fakeAuthDiskSource,
        authTokenInterceptor = authInterceptor,
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(identityService, accountsService, authInterceptor)
    }

    @Test
    fun `rememberedEmailAddress should pull from and update AuthDiskSource`() {
        // AuthDiskSource and the repository start with the same value.
        assertNull(repository.rememberedEmailAddress)
        assertNull(fakeAuthDiskSource.rememberedEmailAddress)

        // Updating the repository updates AuthDiskSource
        repository.rememberedEmailAddress = "remembered@gmail.com"
        assertEquals("remembered@gmail.com", fakeAuthDiskSource.rememberedEmailAddress)

        // Updating AuthDiskSource updates the repository
        fakeAuthDiskSource.rememberedEmailAddress = null
        assertNull(repository.rememberedEmailAddress)
    }

    @Test
    fun `login when pre login fails should return Error with no message`() = runTest {
        coEvery {
            accountsService.preLogin(email = EMAIL)
        } returns (Result.failure(RuntimeException()))
        val result = repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)
        assertEquals(LoginResult.Error(errorMessage = null), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(email = EMAIL) }
    }

    @Test
    fun `login get token fails should return Error with no message`() = runTest {
        coEvery {
            accountsService.preLogin(email = EMAIL)
        } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
            .returns(Result.failure(RuntimeException()))
        val result = repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)
        assertEquals(LoginResult.Error(errorMessage = null), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
    }

    @Test
    fun `login get token returns Invalid should return Error with correct message`() = runTest {
        coEvery {
            accountsService.preLogin(email = EMAIL)
        } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
            .returns(
                Result.success(
                    GetTokenResponseJson.Invalid(
                        errorModel = GetTokenResponseJson.Invalid.ErrorModel(
                            errorMessage = "mock_error_message",
                        ),
                    ),
                ),
            )
        val result = repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)
        assertEquals(LoginResult.Error(errorMessage = "mock_error_message"), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
    }

    @Test
    fun `login get token succeeds should return Success and update AuthState`() = runTest {
        coEvery {
            accountsService.preLogin(email = EMAIL)
        } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
            .returns(Result.success(GetTokenResponseJson.Success(accessToken = ACCESS_TOKEN)))
        every { authInterceptor.authToken = ACCESS_TOKEN } returns Unit
        val result = repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)
        assertEquals(LoginResult.Success, result)
        assertEquals(AuthState.Authenticated(ACCESS_TOKEN), repository.authStateFlow.value)
        verify { authInterceptor.authToken = ACCESS_TOKEN }
        coVerify { accountsService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
    }

    @Test
    fun `login get token returns captcha request should return CaptchaRequired`() = runTest {
        coEvery { accountsService.preLogin(EMAIL) } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
            .returns(Result.success(GetTokenResponseJson.CaptchaRequired(CAPTCHA_KEY)))
        val result = repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)
        assertEquals(LoginResult.CaptchaRequired(CAPTCHA_KEY), result)
        assertEquals(AuthState.Unauthenticated, repository.authStateFlow.value)
        coVerify { accountsService.preLogin(email = EMAIL) }
        coVerify {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
    }

    @Test
    fun `setCaptchaCallbackToken should change the value of captchaTokenFlow`() = runTest {
        repository.captchaTokenResultFlow.test {
            repository.setCaptchaCallbackTokenResult(CaptchaCallbackTokenResult.Success("mockk"))
            assertEquals(
                CaptchaCallbackTokenResult.Success("mockk"),
                awaitItem(),
            )
        }
    }

    @Test
    fun `logout should change AuthState to be Unauthenticated`() = runTest {
        // First login:
        coEvery {
            accountsService.preLogin(email = EMAIL)
        } returns Result.success(PRE_LOGIN_SUCCESS)
        coEvery {
            identityService.getToken(
                email = EMAIL,
                passwordHash = PASSWORD_HASH,
                captchaToken = null,
            )
        }
            .returns(Result.success(GetTokenResponseJson.Success(accessToken = ACCESS_TOKEN)))
        every { authInterceptor.authToken = ACCESS_TOKEN } returns Unit
        repository.login(email = EMAIL, password = PASSWORD, captchaToken = null)

        // Then call logout:
        repository.authStateFlow.test {
            assertEquals(AuthState.Authenticated(ACCESS_TOKEN), awaitItem())
            repository.logout()
            assertEquals(AuthState.Unauthenticated, awaitItem())
        }
    }

    companion object {
        private const val EMAIL = "test@test.com"
        private const val PASSWORD = "password"
        private const val PASSWORD_HASH = "passwordHash"
        private const val ACCESS_TOKEN = "accessToken"
        private const val CAPTCHA_KEY = "captcha"
        private val PRE_LOGIN_SUCCESS = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Pbkdf2(iterations = 1u),
        )
    }
}

private class FakeAuthDiskSource : AuthDiskSource {
    override var rememberedEmailAddress: String? = null
}
