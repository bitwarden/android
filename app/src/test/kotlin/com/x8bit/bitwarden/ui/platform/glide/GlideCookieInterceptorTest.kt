package com.x8bit.bitwarden.ui.platform.glide

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.interceptor.FakeInterceptorChain
import com.bitwarden.network.model.NetworkCookie
import com.bitwarden.network.provider.CookieProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GlideCookieInterceptorTest {

    private val mockCookieProvider: CookieProvider = mockk()

    private val interceptor = GlideCookieInterceptor(
        cookieProvider = mockCookieProvider,
    )

    @Test
    fun `intercept should proceed without cookie header when no cookies available`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns emptyList()

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        assertNull(response.request.header("Cookie"))
    }

    @Test
    fun `intercept should attach single cookie correctly`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns listOf(
            NetworkCookie(name = "awselb", value = "session123"),
        )

        val response = interceptor.intercept(chain)

        assertEquals("awselb=session123", response.request.header("Cookie"))
    }

    @Test
    fun `intercept should attach multiple cookies in correct format`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns listOf(
            NetworkCookie(name = "awselb", value = "session123"),
            NetworkCookie(name = "awselbcors", value = "cors456"),
        )

        val response = interceptor.intercept(chain)

        assertEquals(
            "awselb=session123; awselbcors=cors456",
            response.request.header("Cookie"),
        )
    }

    @Test
    fun `intercept should throw CookieRedirectException on 302 response without cookies`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()

        val redirectResponse = Response.Builder()
            .code(302)
            .message("Found")
            .protocol(Protocol.HTTP_1_1)
            .request(originalRequest)
            .header("Location", "https://idp.example.com/auth")
            .build()

        val chain = FakeInterceptorChain(
            request = originalRequest,
            responseProvider = { redirectResponse },
        )

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns emptyList()

        val exception = assertThrows<CookieRedirectException> {
            interceptor.intercept(chain)
        }

        assertEquals("vault.bitwarden.com", exception.hostname)
    }

    @Test
    fun `intercept should throw CookieRedirectException on 302 response with cookies`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()

        val redirectResponse = Response.Builder()
            .code(302)
            .message("Found")
            .protocol(Protocol.HTTP_1_1)
            .request(originalRequest)
            .header("Location", "https://idp.example.com/auth")
            .build()

        val chain = FakeInterceptorChain(
            request = originalRequest,
            responseProvider = { redirectResponse },
        )

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns listOf(
            NetworkCookie(name = "awselb", value = "session123"),
        )

        val exception = assertThrows<CookieRedirectException> {
            interceptor.intercept(chain)
        }

        assertEquals("vault.bitwarden.com", exception.hostname)
    }

    @Test
    fun `intercept should not call needsBootstrap or acquireCookies`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/icons/icon.png")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every {
            mockCookieProvider.getCookies("vault.bitwarden.com")
        } returns emptyList()

        interceptor.intercept(chain)

        verify(exactly = 0) {
            mockCookieProvider.needsBootstrap(any())
            mockCookieProvider.acquireCookies(any())
        }
    }
}
