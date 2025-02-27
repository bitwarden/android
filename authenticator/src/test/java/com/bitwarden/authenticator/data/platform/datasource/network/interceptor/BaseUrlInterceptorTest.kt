package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class BaseUrlInterceptorTest {
    private val baseUrlInterceptor = BaseUrlInterceptor()

    @Test
    fun `intercept with a null base URL should proceed with the original request`() {
        val request = Request.Builder().url("http://www.fake.com/").build()
        val chain = FakeInterceptorChain(request)

        val response = baseUrlInterceptor.intercept(chain)

        assertEquals(request, response.request)
        assertEquals("http", response.request.url.scheme)
        assertEquals("www.fake.com", response.request.url.host)
    }

    @Test
    fun `intercept with a non-null base URL should update the base URL used by the request`() {
        baseUrlInterceptor.baseUrl = "https://api.bitwarden.com"

        val request = Request.Builder().url("http://www.fake.com/").build()
        val chain = FakeInterceptorChain(request)

        val response = baseUrlInterceptor.intercept(chain)

        assertNotEquals(request, response.request)
        assertEquals("https", response.request.url.scheme)
        assertEquals("api.bitwarden.com", response.request.url.host)
    }
}
