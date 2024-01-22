package com.x8bit.bitwarden.data.platform.datasource.network.authenticator

import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.repository.model.JwtTokenDataJson
import com.x8bit.bitwarden.data.auth.repository.util.parseJwtTokenDataOrNull
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
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
    private val authenticatorProvider: AuthenticatorProvider = mockk()

    @BeforeEach
    fun setup() {
        authenticator = RefreshAuthenticator()
        authenticator.authenticatorProvider = authenticatorProvider

        mockkStatic(::parseJwtTokenDataOrNull)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::parseJwtTokenDataOrNull)
    }

    @Test
    fun `RefreshAuthenticator returns null if the request is for a different user`() {
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        every { authenticatorProvider.activeUserId } returns "different_user_id"

        assertNull(authenticator.authenticate(null, RESPONSE_401))

        verify(exactly = 1) {
            authenticatorProvider.activeUserId
        }
    }

    @Test
    fun `RefreshAuthenticator returns null if API has no authorization user ID`() {
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns null

        assertNull(authenticator.authenticate(null, RESPONSE_401))

        verify(exactly = 0) {
            authenticatorProvider.activeUserId
            authenticatorProvider.refreshAccessTokenSynchronously(any())
            authenticatorProvider.logout(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RefreshAuthenticator returns null and logs out when request is for active user and refresh is failure`() {
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        every { authenticatorProvider.activeUserId } returns USER_ID
        every {
            authenticatorProvider.refreshAccessTokenSynchronously(USER_ID)
        } returns Throwable("Fail").asFailure()
        every { authenticatorProvider.logout(USER_ID) } just runs

        assertNull(authenticator.authenticate(null, RESPONSE_401))

        verify(exactly = 1) {
            authenticatorProvider.activeUserId
            authenticatorProvider.refreshAccessTokenSynchronously(USER_ID)
            authenticatorProvider.logout(USER_ID)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RefreshAuthenticator returns updated request when request is for active user and refresh is success`() {
        val newAccessToken = "newAccessToken"
        val refreshResponse = RefreshTokenResponseJson(
            accessToken = newAccessToken,
            expiresIn = 3600,
            refreshToken = "refreshToken",
            tokenType = "Bearer",
        )
        every { parseJwtTokenDataOrNull(JWT_ACCESS_TOKEN) } returns JTW_TOKEN
        every { authenticatorProvider.activeUserId } returns USER_ID
        every {
            authenticatorProvider.refreshAccessTokenSynchronously(USER_ID)
        } returns refreshResponse.asSuccess()

        val authenticatedRequest = authenticator.authenticate(null, RESPONSE_401)

        // The okhttp3 Request is not a data class and does not implement equals
        // so we are manually checking that the correct header is added.
        assertEquals(
            "Bearer $newAccessToken",
            authenticatedRequest!!.header("Authorization"),
        )
        verify(exactly = 1) {
            authenticatorProvider.activeUserId
            authenticatorProvider.refreshAccessTokenSynchronously(USER_ID)
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
