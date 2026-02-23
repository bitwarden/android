package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class CookieUtilsTest {

    private val mockUri: Uri = mockk(relaxed = true) {
        every { scheme } returns "bitwarden"
        every { host } returns "sso-cookie-vendor"
    }

    private val mockIntent: Intent = mockk(relaxed = true) {
        every { action } returns Intent.ACTION_VIEW
        every { data } returns mockUri
    }

    @Test
    fun `getCookieCallbackResultOrNull should return null when action is not ACTION_VIEW`() {
        every { mockIntent.action } returns Intent.ACTION_MAIN
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertNull(result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should return null when data is null`() {
        every { mockIntent.data } returns null
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertNull(result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should return null when scheme is wrong`() {
        every { mockUri.scheme } returns "https"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertNull(result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should return null when host is wrong`() {
        every { mockUri.host } returns "sso-callback"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertNull(result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should delegate cookie parsing to Uri`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB")
        every { mockUri.getQueryParameter("AWSELB") } returns "abc123"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("AWSELB" to "abc123"),
            ),
            result,
        )
    }

    @Test
    fun `getCookieCallbackResult should return MissingCookie when URI is null`() {
        val uri: Uri? = null
        assertEquals(
            CookieCallbackResult.MissingCookie,
            uri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should return MissingCookie when no query parameters`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns emptySet()
        }
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should return MissingCookie when only d parameter is present`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("d")
            every { getQueryParameter("d") } returns "1"
        }
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should extract dynamic cookie parameter name`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("sessionToken")
            every { getQueryParameter("sessionToken") } returns "abc123"
        }
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("sessionToken" to "abc123"),
            ),
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should extract multiple cookie parameters`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("AWSELB-0", "AWSELB-1", "AWSELB-2")
            every { getQueryParameter("AWSELB-0") } returns "part0"
            every { getQueryParameter("AWSELB-1") } returns "part1"
            every { getQueryParameter("AWSELB-2") } returns "part2"
        }
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf(
                    "AWSELB-0" to "part0",
                    "AWSELB-1" to "part1",
                    "AWSELB-2" to "part2",
                ),
            ),
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should filter out d parameter from cookies`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("sessionToken", "d")
            every { getQueryParameter("sessionToken") } returns "abc123"
            every { getQueryParameter("d") } returns "1"
        }
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("sessionToken" to "abc123"),
            ),
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should return MissingCookie when cookie value is empty`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("sessionToken")
            every { getQueryParameter("sessionToken") } returns ""
        }
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should return MissingCookie when cookie value is null`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("sessionToken")
            every { getQueryParameter("sessionToken") } returns null
        }
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockUri.getCookieCallbackResult(),
        )
    }

    @Test
    fun `getCookieCallbackResult should handle mixed valid and empty cookies`() {
        val mockUri = mockk<Uri> {
            every { queryParameterNames } returns setOf("validCookie", "emptyCookie")
            every { getQueryParameter("validCookie") } returns "value"
            every { getQueryParameter("emptyCookie") } returns ""
        }
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("validCookie" to "value"),
            ),
            mockUri.getCookieCallbackResult(),
        )
    }
}
