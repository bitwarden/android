package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class CookieUtilsTest {

    private lateinit var mockUri: Uri
    private lateinit var mockIntent: Intent

    @BeforeEach
    fun setUp() {
        mockUri = mockk(relaxed = true) {
            every { scheme } returns "bitwarden"
            every { host } returns "sso_cookie_vendor"
        }
        mockIntent = mockk(relaxed = true) {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns mockUri
        }
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
    fun `getCookieCallbackResultOrNull should return MissingCookie when no query parameters`() {
        every { mockUri.queryParameterNames } returns emptySet()
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(CookieCallbackResult.MissingCookie, result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should return MissingCookie with only d parameter`() {
        every { mockUri.queryParameterNames } returns setOf("d")
        every { mockUri.getQueryParameter("d") } returns "1"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(CookieCallbackResult.MissingCookie, result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should parse single cookie correctly`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB")
        every { mockUri.getQueryParameter("AWSELB") } returns "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("AWSELB" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"),
            ),
            result,
        )
    }

    @Test
    fun `getCookieCallbackResultOrNull should parse sharded cookies correctly`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB-0", "AWSELB-1", "AWSELB-2")
        every { mockUri.getQueryParameter("AWSELB-0") } returns "part0"
        every { mockUri.getQueryParameter("AWSELB-1") } returns "part1"
        every { mockUri.getQueryParameter("AWSELB-2") } returns "part2"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf(
                    "AWSELB-0" to "part0",
                    "AWSELB-1" to "part1",
                    "AWSELB-2" to "part2",
                ),
            ),
            result,
        )
    }

    @Test
    fun `getCookieCallbackResultOrNull should filter out d parameter from sharded cookies`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB-0", "AWSELB-1", "d")
        every { mockUri.getQueryParameter("AWSELB-0") } returns "part0"
        every { mockUri.getQueryParameter("AWSELB-1") } returns "part1"
        every { mockUri.getQueryParameter("d") } returns "1"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf(
                    "AWSELB-0" to "part0",
                    "AWSELB-1" to "part1",
                ),
            ),
            result,
        )
    }

    @Test
    fun `getCookieCallbackResultOrNull should handle empty cookie value as missing`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB")
        every { mockUri.getQueryParameter("AWSELB") } returns ""
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(CookieCallbackResult.MissingCookie, result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should handle null cookie value as missing`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB")
        every { mockUri.getQueryParameter("AWSELB") } returns null
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(CookieCallbackResult.MissingCookie, result)
    }

    @Test
    fun `getCookieCallbackResultOrNull should handle multiple different cookies`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB", "SESSION_ID", "XSRF_TOKEN")
        every { mockUri.getQueryParameter("AWSELB") } returns "cookie1"
        every { mockUri.getQueryParameter("SESSION_ID") } returns "cookie2"
        every { mockUri.getQueryParameter("XSRF_TOKEN") } returns "cookie3"
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf(
                    "AWSELB" to "cookie1",
                    "SESSION_ID" to "cookie2",
                    "XSRF_TOKEN" to "cookie3",
                ),
            ),
            result,
        )
    }

    @Test
    fun `getCookieCallbackResultOrNull should handle mixed valid and empty cookies`() {
        every { mockUri.queryParameterNames } returns setOf("AWSELB", "EMPTY_COOKIE")
        every { mockUri.getQueryParameter("AWSELB") } returns "validValue"
        every { mockUri.getQueryParameter("EMPTY_COOKIE") } returns ""
        val result = mockIntent.getCookieCallbackResultOrNull()
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("AWSELB" to "validValue"),
            ),
            result,
        )
    }
}
