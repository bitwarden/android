package com.bitwarden.network.interceptor

import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import okhttp3.Request
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptorTest {
    private val mockAuthTokenProvider = mockk<AuthTokenProvider> {
        every { getActiveAccessTokenOrNull() } returns null
    }
    private val interceptor: AuthTokenInterceptor = AuthTokenInterceptor(
        authTokenProvider = mockAuthTokenProvider,
    )
    private val request: Request = Request
        .Builder()
        .url("http://localhost")
        .build()

    @Test
    fun `intercept should add the auth token when set`() {
        every { mockAuthTokenProvider.getActiveAccessTokenOrNull() } returns ACCESS_TOKEN

        val response = interceptor.intercept(
            chain = FakeInterceptorChain(request = request),
        )
        assertEquals(
            "Bearer $ACCESS_TOKEN",
            response.request.header("Authorization"),
        )
    }

    @Test
    fun `intercept should throw an exception when an auth token is missing`() {
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

private const val ACCESS_TOKEN: String = "access_token"
