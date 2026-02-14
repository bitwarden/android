package com.bitwarden.network.interceptor

import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.model.NetworkCookie
import com.bitwarden.network.provider.CookieProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CookieInterceptorTest {

    private val mockCookieProvider: CookieProvider = mockk {
        every { acquireCookies(any()) } just runs
    }

    private val interceptor = CookieInterceptor(
        cookieProvider = mockCookieProvider,
    )

    @Test
    fun `intercept should skip cookie handling for config path`() {
        val originalRequest = Request.Builder()
            .url("https://api.bitwarden.com/api/config")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        verify(exactly = 0) {
            mockCookieProvider.needsBootstrap(any())
            mockCookieProvider.getCookies(any())
        }
    }

    @Test
    fun `intercept should skip cookie handling for sso-cookie-vendor path`() {
        val originalRequest = Request.Builder()
            .url("https://api.bitwarden.com/api/sso-cookie-vendor")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        verify(exactly = 0) {
            mockCookieProvider.needsBootstrap(any())
            mockCookieProvider.getCookies(any())
        }
    }

    @Test
    fun `intercept should throw CookieRedirectException when bootstrap is needed`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns true

        val exception = assertThrows<CookieRedirectException> {
            interceptor.intercept(chain)
        }

        assertEquals("vault.bitwarden.com", exception.hostname)
        assertNull(exception.location)
        verify { mockCookieProvider.acquireCookies("vault.bitwarden.com") }
        verify(exactly = 0) { mockCookieProvider.getCookies(any()) }
    }

    @Test
    fun `intercept should proceed without cookies when no cookies available`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns emptyList()

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        assertNull(response.request.header("Cookie"))
        verify(exactly = 1) { mockCookieProvider.getCookies("vault.bitwarden.com") }
    }

    @Test
    fun `intercept should attach single cookie when cookies available`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val cookies = listOf(
            NetworkCookie(name = "awselb", value = "session123"),
        )

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns cookies

        val response = interceptor.intercept(chain)

        assertEquals("awselb=session123", response.request.header("Cookie"))
        verify(exactly = 1) { mockCookieProvider.getCookies("vault.bitwarden.com") }
    }

    @Test
    fun `intercept should attach multiple cookies with correct format`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val cookies = listOf(
            NetworkCookie(name = "awselb", value = "session123"),
            NetworkCookie(name = "awselbcors", value = "cors456"),
            NetworkCookie(name = "custom", value = "value789"),
        )

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns cookies

        val response = interceptor.intercept(chain)

        assertEquals(
            "awselb=session123; awselbcors=cors456; custom=value789",
            response.request.header("Cookie"),
        )
        verify(exactly = 1) { mockCookieProvider.getCookies("vault.bitwarden.com") }
    }

    @Test
    fun `intercept should throw CookieRedirectException on 302 response`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
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

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns emptyList()

        val exception = assertThrows<CookieRedirectException> {
            interceptor.intercept(chain)
        }

        assertEquals("vault.bitwarden.com", exception.hostname)
        assertEquals("https://idp.example.com/auth", exception.location)
        verify { mockCookieProvider.acquireCookies("vault.bitwarden.com") }
    }

    @Test
    fun `intercept should return 302 response as-is when no Location header is present`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()

        val redirectResponse = Response.Builder()
            .code(302)
            .message("Found")
            .protocol(Protocol.HTTP_1_1)
            .request(originalRequest)
            .build()

        val chain = FakeInterceptorChain(
            request = originalRequest,
            responseProvider = { redirectResponse },
        )

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns emptyList()

        val response = interceptor.intercept(chain)

        assertEquals(302, response.code)
        verify(exactly = 0) { mockCookieProvider.acquireCookies(any()) }
    }

    @Test
    fun `intercept should not throw exception on successful 200 response`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns emptyList()

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
    }

    @Test
    fun `intercept should skip cookie handling for config subpath`() {
        val originalRequest = Request.Builder()
            .url("https://api.bitwarden.com/api/config/sub-path")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        verify(exactly = 0) {
            mockCookieProvider.needsBootstrap(any())
            mockCookieProvider.getCookies(any())
        }
    }

    @Test
    fun `intercept should skip cookie handling for path that starts with excluded prefix`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/configuration")
            .build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = interceptor.intercept(chain)

        assertEquals(originalRequest, response.request)
        verify(exactly = 0) {
            mockCookieProvider.needsBootstrap(any())
            mockCookieProvider.getCookies(any())
        }
    }

    @Test
    fun `intercept should throw CookieRedirectException on 302 when cookies were attached`() {
        val originalRequest = Request.Builder()
            .url("https://vault.bitwarden.com/api/accounts/profile")
            .build()

        val cookies = listOf(
            NetworkCookie(name = "awselb", value = "session123"),
        )

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

        every { mockCookieProvider.needsBootstrap("vault.bitwarden.com") } returns false
        every { mockCookieProvider.getCookies("vault.bitwarden.com") } returns cookies

        val exception = assertThrows<CookieRedirectException> {
            interceptor.intercept(chain)
        }

        assertEquals("vault.bitwarden.com", exception.hostname)
        assertEquals("https://idp.example.com/auth", exception.location)
        verify { mockCookieProvider.acquireCookies("vault.bitwarden.com") }
    }
}
