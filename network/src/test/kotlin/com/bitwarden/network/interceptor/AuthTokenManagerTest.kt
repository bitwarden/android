package com.bitwarden.network.interceptor

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.AuthTokenData
import com.bitwarden.network.model.JwtTokenDataJson
import com.bitwarden.network.provider.RefreshTokenProvider
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthTokenManagerTest {
    private val mockAuthTokenProvider = mockk<AuthTokenProvider> {
        every { getAuthTokenDataOrNull() } returns null
    }
    private val refreshTokenProvider: RefreshTokenProvider = mockk()
    private val authTokenManager: AuthTokenManager = AuthTokenManager(
        clock = FIXED_CLOCK,
        authTokenProvider = mockAuthTokenProvider,
    )
    private val request: Request = Request
        .Builder()
        .url("http://localhost")
        .build()

    @BeforeEach
    fun setup() {
        authTokenManager.refreshTokenProvider = refreshTokenProvider
        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
    }

    @Nested
    inner class TokenProvider {
        @Test
        fun `returns null if token provider has no auth data for user ID`() {
            val userId = "userId"
            every { mockAuthTokenProvider.getAuthTokenDataOrNull(userId = userId) } returns null
            val result = authTokenManager.getAccessToken(userId = userId)
            assertNull(result)
        }

        @Test
        fun `returns null if refresh fails`() {
            val userId = "userId"
            val authData = AuthTokenData(
                userId = userId,
                accessToken = ACCESS_TOKEN,
                expiresAtSec = FIXED_CLOCK.instant().epochSecond,
            )
            every { mockAuthTokenProvider.getAuthTokenDataOrNull(userId = userId) } returns authData
            every {
                refreshTokenProvider.refreshAccessTokenSynchronously(userId = userId)
            } returns Throwable("Fail!").asFailure()
            val result = authTokenManager.getAccessToken(userId = userId)
            assertNull(result)
        }

        @Test
        fun `returns access token if refresh is not required`() {
            val userId = "userId"
            val authData = AuthTokenData(
                userId = userId,
                accessToken = ACCESS_TOKEN,
                expiresAtSec = 0L,
            )
            val refreshedAccessToken = "refreshed_access_token"
            every { mockAuthTokenProvider.getAuthTokenDataOrNull(userId = userId) } returns authData
            every {
                refreshTokenProvider.refreshAccessTokenSynchronously(userId = userId)
            } returns refreshedAccessToken.asSuccess()
            val result = authTokenManager.getAccessToken(userId = userId)
            assertEquals(refreshedAccessToken, result)
        }
    }

    @Nested
    inner class Authenticator {
        @Test
        fun `returns null if API has no authorization user ID`() {
            every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns null

            assertNull(authTokenManager.authenticate(null, RESPONSE_401))

            verify(exactly = 0) {
                refreshTokenProvider.refreshAccessTokenSynchronously(any())
            }
        }

        @Test
        fun `returns null when refresh is failure`() {
            every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
            every {
                refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
            } returns Throwable("Fail").asFailure()

            assertNull(authTokenManager.authenticate(null, RESPONSE_401))

            verify(exactly = 1) {
                refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
            }
        }

        @Test
        fun `returns null when refreshTokenProvider is null`() {
            authTokenManager.refreshTokenProvider = null
            every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
            assertNull(authTokenManager.authenticate(null, RESPONSE_401))
            verify(exactly = 0) {
                refreshTokenProvider.refreshAccessTokenSynchronously(any())
            }
        }

        @Test
        fun `returns updated request when refresh is success`() {
            val newAccessToken = "newAccessToken"
            every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
            every {
                refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
            } returns newAccessToken.asSuccess()

            val authenticatedRequest = authTokenManager.authenticate(null, RESPONSE_401)

            // The okhttp3 Request is not a data class and does not implement equals
            // so we are manually checking that the correct header is added.
            assertEquals(
                "Bearer $newAccessToken",
                authenticatedRequest!!.header("Authorization"),
            )
            verify(exactly = 1) {
                refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
            }
        }
    }

    @Nested
    inner class Interceptor {
        @Test
        fun `intercept should add the auth token when set`() {
            val authTokenData = AuthTokenData(
                userId = USER_ID,
                accessToken = ACCESS_TOKEN,
                expiresAtSec = FIXED_CLOCK.instant().epochSecond + 3600L,
            )
            every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

            val response = authTokenManager.intercept(
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
            authTokenManager.refreshTokenProvider = null
            every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData
            val throwable = assertThrows(IOException::class.java) {
                authTokenManager.intercept(
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
        fun `intercept should throw an io exception when auth token is expired and refreshAccessTokenSynchronously returns an error`() {
            val errorMessage = "Fail!"
            authTokenManager.refreshTokenProvider = object : RefreshTokenProvider {
                override fun refreshAccessTokenSynchronously(
                    userId: String,
                ): Result<String> = IOException(errorMessage).asFailure()
            }
            val authTokenData = AuthTokenData(
                userId = USER_ID,
                accessToken = ACCESS_TOKEN,
                expiresAtSec = FIXED_CLOCK.instant().epochSecond - 3600L,
            )
            every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

            val throwable = assertThrows(IOException::class.java) {
                authTokenManager.intercept(
                    chain = FakeInterceptorChain(request = request),
                )
            }
            assertEquals(errorMessage, throwable.message)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `intercept should throw a http exception when auth token is expired and refreshAccessTokenSynchronously returns an error`() {
            val error = mockk<HttpException>()
            authTokenManager.refreshTokenProvider = object : RefreshTokenProvider {
                override fun refreshAccessTokenSynchronously(
                    userId: String,
                ): Result<String> = error.asFailure()
            }
            val authTokenData = AuthTokenData(
                userId = USER_ID,
                accessToken = ACCESS_TOKEN,
                expiresAtSec = FIXED_CLOCK.instant().epochSecond - 3600L,
            )
            every { mockAuthTokenProvider.getAuthTokenDataOrNull() } returns authTokenData

            val throwable = assertThrows(IOException::class.java) {
                authTokenManager.intercept(
                    chain = FakeInterceptorChain(request = request),
                )
            }
            assertEquals(throwable.cause, error)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `intercept should add the auth token when auth token is expired and refreshAccessTokenSynchronously returns new token`() {
            val token = "token"
            authTokenManager.refreshTokenProvider = object : RefreshTokenProvider {
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

            val response = authTokenManager.intercept(
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
                authTokenManager.intercept(
                    chain = FakeInterceptorChain(request = request),
                )
            }
            assertEquals(
                "Auth token is missing!",
                throwable.cause?.message,
            )
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val USER_ID: String = "user_id"
private const val ACCESS_TOKEN: String = "access_token"

private val JTW_TOKEN = JwtTokenDataJson(
    userId = USER_ID,
    email = "test@bitwarden.com",
    isEmailVerified = true,
    name = "Bitwarden Tester",
    expirationAsEpochTime = 1697495714,
    hasPremium = false,
    authenticationMethodsReference = listOf("Application"),
)

private const val JWT_ACCESS_TOKEN = "jwt"

private val RESPONSE_401 = Response.Builder()
    .code(401)
    .request(
        request = Request.Builder()
            .header(name = "Authorization", value = "Bearer $JWT_ACCESS_TOKEN")
            .url("https://www.bitwarden.com")
            .build(),
    )
    .protocol(Protocol.HTTP_2)
    .message("Unauthenticated")
    .build()
