package com.bitwarden.network.interceptor

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.AuthTokenData
import com.bitwarden.network.provider.RefreshTokenProvider
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import okhttp3.Request
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthTokenInterceptorTest {
    private val mockAuthTokenProvider = mockk<AuthTokenProvider> {
        every { getAuthTokenDataOrNull() } returns null
    }
    private val interceptor: AuthTokenInterceptor = AuthTokenInterceptor(
        clock = FIXED_CLOCK,
        authTokenProvider = mockAuthTokenProvider,
    )
    private val request: Request = Request
        .Builder()
        .url("http://localhost")
        .build()

    @Test
    fun `intercept should add the auth token when set`() {
        val authTokenData = AuthTokenData(
            userId = USER_ID,
            accessToken = ACCESS_TOKEN,
            expiresAtSec = FIXED_CLOCK.instant().epochSecond + 3600L,
        )
        every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

        val response = interceptor.intercept(
            chain = FakeInterceptorChain(request = request),
        )
        assertEquals(
            "Bearer $ACCESS_TOKEN",
            response.request.header("Authorization"),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `intercept should throw an exception when auth token is expired and refreshTokenProvider is missing`() {
        val authTokenData = AuthTokenData(
            userId = USER_ID,
            accessToken = ACCESS_TOKEN,
            expiresAtSec = FIXED_CLOCK.instant().epochSecond - 3600L,
        )
        every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

        val throwable = assertThrows(IOException::class.java) {
            interceptor.intercept(
                chain = FakeInterceptorChain(request = request),
            )
        }
        assertEquals(
            "Refresh token provider is missing!",
            throwable.cause?.message,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `intercept should throw an exception when auth token is expired and refreshAccessTokenSynchronously returns an error`() {
        val errorMessage = "Fail!"
        interceptor.refreshTokenProvider = object : RefreshTokenProvider {
            override fun refreshAccessTokenSynchronously(
                userId: String,
            ): Result<String> = Throwable(errorMessage).asFailure()
        }
        val authTokenData = AuthTokenData(
            userId = USER_ID,
            accessToken = ACCESS_TOKEN,
            expiresAtSec = FIXED_CLOCK.instant().epochSecond - 3600L,
        )
        every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

        val throwable = assertThrows(IOException::class.java) {
            interceptor.intercept(
                chain = FakeInterceptorChain(request = request),
            )
        }
        assertEquals(errorMessage, throwable.cause?.message)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `intercept should add the auth token when auth token is expired and refreshAccessTokenSynchronously returns new token`() {
        val token = "token"
        interceptor.refreshTokenProvider = object : RefreshTokenProvider {
            override fun refreshAccessTokenSynchronously(
                userId: String,
            ): Result<String> = token.asSuccess()
        }
        val authTokenData = AuthTokenData(
            userId = USER_ID,
            accessToken = ACCESS_TOKEN,
            expiresAtSec = FIXED_CLOCK.instant().epochSecond - 3600L,
        )
        every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

        val response = interceptor.intercept(
            chain = FakeInterceptorChain(request = request),
        )
        assertEquals(
            "Bearer $token",
            response.request.header("Authorization"),
        )
    }

    @Test
    fun `intercept should throw an exception when an auth token data is missing`() {
        val throwable = assertThrows(IOException::class.java) {
            interceptor.intercept(
                chain = FakeInterceptorChain(request = request),
            )
        }
        assertEquals(
            "Auth token is missing!",
            throwable.cause?.message,
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val USER_ID: String = "user_id"
private const val ACCESS_TOKEN: String = "access_token"
