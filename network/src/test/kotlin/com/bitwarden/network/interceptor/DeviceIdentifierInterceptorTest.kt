package com.bitwarden.network.interceptor

import com.bitwarden.network.provider.AppIdProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.Headers
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceIdentifierInterceptorTest {
    private val appIdProvider = mockk<AppIdProvider> {
        every { uniqueAppId } returns UNIQUE_APP_ID
    }
    private val interceptor = DeviceIdentifierInterceptor(appIdProvider = appIdProvider)

    @Test
    fun `intercept should add the device identifier header to the request`() {
        val originalRequest = Request.Builder().url("http://www.fake.com/").build()
        val chain = FakeInterceptorChain(request = originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(
            Headers.headersOf("Device-Identifier", UNIQUE_APP_ID),
            response.request.headers,
        )
    }

    @Test
    fun `intercept should not add the device identifier header when authorization is present`() {
        val originalRequest = Request.Builder()
            .url("http://www.fake.com/")
            .header(name = "Authorization", value = "Bearer token")
            .build()
        val chain = FakeInterceptorChain(request = originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(
            Headers.headersOf("Authorization", "Bearer token"),
            response.request.headers,
        )
    }

    @Test
    fun `intercept should replace an existing device identifier header`() {
        val originalRequest = Request.Builder()
            .url("http://www.fake.com/")
            .header(name = "Device-Identifier", value = "existing-id")
            .build()
        val chain = FakeInterceptorChain(request = originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(
            Headers.headersOf("Device-Identifier", UNIQUE_APP_ID),
            response.request.headers,
        )
    }
}

private const val UNIQUE_APP_ID: String = "mockUniqueAppId"
