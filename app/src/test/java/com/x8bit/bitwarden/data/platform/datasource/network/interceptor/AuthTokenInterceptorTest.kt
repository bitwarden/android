package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import junit.framework.TestCase.assertEquals
import okhttp3.Request
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptorTest {
    private val interceptor: AuthTokenInterceptor = AuthTokenInterceptor()
    private val mockAuthToken = "yourAuthToken"
    private val request: Request = Request
        .Builder()
        .url("http://localhost")
        .build()

    @Test
    fun `intercept should add the auth token when set`() {
        interceptor.authToken = mockAuthToken
        val response = interceptor.intercept(
            chain = FakeInterceptorChain(request = request),
        )
        assertEquals(
            "Bearer $mockAuthToken",
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
