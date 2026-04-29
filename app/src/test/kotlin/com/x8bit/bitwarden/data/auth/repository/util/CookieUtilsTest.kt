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

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should return MissingCookie when no query parameters`() {
        every { mockUri.queryParameterNames } returns emptySet()
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should return MissingCookie when only d parameter is present`() {
        every { mockUri.queryParameterNames } returns setOf("d")
        every { mockUri.getQueryParameter("d") } returns "1"
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should extract dynamic cookie parameter name`() {
        every { mockUri.queryParameterNames } returns setOf("sessionToken")
        every { mockUri.getQueryParameter("sessionToken") } returns "abc123"
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("sessionToken" to "abc123"),
            ),
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Test
    fun `getCookieCallbackResultOrNull should extract multiple cookie parameters`() {
        every { mockUri.queryParameterNames } returns setOf(
            "AWSELB-0",
            "AWSELB-1",
            "AWSELB-2",
        )
        every { mockUri.getQueryParameter("AWSELB-0") } returns "part0"
        every { mockUri.getQueryParameter("AWSELB-1") } returns "part1"
        every { mockUri.getQueryParameter("AWSELB-2") } returns "part2"
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf(
                    "AWSELB-0" to "part0",
                    "AWSELB-1" to "part1",
                    "AWSELB-2" to "part2",
                ),
            ),
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should filter out d parameter from cookies`() {
        every { mockUri.queryParameterNames } returns setOf("sessionToken", "d")
        every { mockUri.getQueryParameter("sessionToken") } returns "abc123"
        every { mockUri.getQueryParameter("d") } returns "1"
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("sessionToken" to "abc123"),
            ),
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should return MissingCookie when cookie value is empty`() {
        every { mockUri.queryParameterNames } returns setOf("sessionToken")
        every { mockUri.getQueryParameter("sessionToken") } returns ""
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should return MissingCookie when cookie value is null`() {
        every { mockUri.queryParameterNames } returns setOf("sessionToken")
        every { mockUri.getQueryParameter("sessionToken") } returns null
        assertEquals(
            CookieCallbackResult.MissingCookie,
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCookieCallbackResultOrNull should handle mixed valid and empty cookies`() {
        every { mockUri.queryParameterNames } returns setOf(
            "validCookie",
            "emptyCookie",
        )
        every { mockUri.getQueryParameter("validCookie") } returns "value"
        every { mockUri.getQueryParameter("emptyCookie") } returns ""
        assertEquals(
            CookieCallbackResult.Success(
                cookies = mapOf("validCookie" to "value"),
            ),
            mockIntent.getCookieCallbackResultOrNull(),
        )
    }
}
