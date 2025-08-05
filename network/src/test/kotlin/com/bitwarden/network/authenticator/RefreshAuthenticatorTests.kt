package com.bitwarden.network.authenticator

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefreshAuthenticatorTests {
    private lateinit var authenticator: RefreshAuthenticator
    private val refreshTokenProvider: RefreshTokenProvider = mockk()

    @BeforeEach
    fun setup() {
        authenticator = RefreshAuthenticator()
        authenticator.refreshTokenProvider = refreshTokenProvider

        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
    }

    @Test
    fun `RefreshAuthenticator returns null if API has no authorization user ID`() {
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns null

        assertNull(authenticator.authenticate(null, RESPONSE_401))

        verify(exactly = 0) {
            refreshTokenProvider.refreshAccessTokenSynchronously(any())
        }
    }

    @Test
    fun `RefreshAuthenticator returns null when refresh is failure`() {
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        every {
            refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
        } returns Throwable("Fail").asFailure()

        assertNull(authenticator.authenticate(null, RESPONSE_401))

        verify(exactly = 1) {
            refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
        }
    }

    @Test
    fun `RefreshAuthenticator returns null when refreshTokenProvider is null`() {
        authenticator.refreshTokenProvider = null
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        assertNull(authenticator.authenticate(null, RESPONSE_401))
        verify(exactly = 0) {
            refreshTokenProvider.refreshAccessTokenSynchronously(any())
        }
    }

    @Test
    fun `RefreshAuthenticator returns updated request when refresh is success`() {
        val newAccessToken = "newAccessToken"
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        every {
            refreshTokenProvider.refreshAccessTokenSynchronously(USER_ID)
        } returns newAccessToken.asSuccess()

        val authenticatedRequest = authenticator.authenticate(null, RESPONSE_401)

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

private const val USER_ID = "2a135b23-e1fb-42c9-bec3-573857bc8181"

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
