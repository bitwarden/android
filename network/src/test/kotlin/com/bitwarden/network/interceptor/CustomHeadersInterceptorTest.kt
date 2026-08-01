package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.CustomHeadersProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomHeadersInterceptorTest {

    private val mockCustomHeadersProvider: CustomHeadersProvider = mockk()

    private val interceptor = CustomHeadersInterceptor(
        customHeadersProvider = mockCustomHeadersProvider,
    )

    @Test
    fun `intercept should add the provided custom headers to the request`() {
        every {
            mockCustomHeadersProvider.getCustomHeaders(url = "https://vault.example.com/api/sync")
        } returns mapOf(
            "CF-Access-Client-Id" to "client-id",
            "CF-Access-Client-Secret" to "client-secret",
        )
        val originalRequest = Request.Builder()
            .url("https://vault.example.com/api/sync")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals("client-id", response.request.header("CF-Access-Client-Id"))
        assertEquals("client-secret", response.request.header("CF-Access-Client-Secret"))
    }

    @Test
    fun `intercept should preserve existing request headers when adding custom headers`() {
        every {
            mockCustomHeadersProvider.getCustomHeaders(url = any())
        } returns mapOf("Custom-Header" to "value")
        val originalRequest = Request.Builder()
            .url("https://vault.example.com/api/sync")
            .header("Bitwarden-Client-Name", "mobile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals("mobile", response.request.header("Bitwarden-Client-Name"))
        assertEquals("value", response.request.header("Custom-Header"))
    }

    @Test
    fun `intercept should leave the request unchanged when there are no custom headers`() {
        every { mockCustomHeadersProvider.getCustomHeaders(url = any()) } returns emptyMap()
        val originalRequest = Request.Builder()
            .url("https://api.pwnedpasswords.com/range/12345")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        assertNull(response.request.header("CF-Access-Client-Secret"))
    }
}
