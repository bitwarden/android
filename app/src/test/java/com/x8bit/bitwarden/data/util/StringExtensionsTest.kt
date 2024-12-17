package com.x8bit.bitwarden.data.util

import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import com.x8bit.bitwarden.data.platform.util.findLastSubstringIndicesOrNull
import com.x8bit.bitwarden.data.platform.util.getDomainOrNull
import com.x8bit.bitwarden.data.platform.util.getHostOrNull
import com.x8bit.bitwarden.data.platform.util.getHostWithPortOrNull
import com.x8bit.bitwarden.data.platform.util.getWebHostFromAndroidUriOrNull
import com.x8bit.bitwarden.data.platform.util.hasHttpProtocol
import com.x8bit.bitwarden.data.platform.util.hasPort
import com.x8bit.bitwarden.data.platform.util.isAndroidApp
import com.x8bit.bitwarden.data.platform.util.parseDomainOrNull
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

class StringExtensionsTest {

    @AfterEach
    fun teardown() {
        unmockkStatic(URI::parseDomainOrNull)
    }

    @Test
    fun `toUriOrNull should return null when uri is malformed`() {
        assertNull("not a uri".toUriOrNull())
    }

    @Test
    fun `toUriOrNull should return URI when uri is valid`() {
        assertNotNull("www.google.com".toUriOrNull())
    }

    @Test
    fun `isAndroidApp should return true when string starts with android app protocol`() {
        assertTrue("androidapp://com.x8bit.bitwarden".isAndroidApp())
    }

    @Test
    fun `isAndroidApp should return false when doesn't start with android app protocol`() {
        assertFalse("com.x8bit.bitwarden".isAndroidApp())
    }

    @Test
    fun `hasHttpProtocol should return false when doesn't start with an appropriate protocol`() {
        assertFalse("androidapp://com.x8bit.bitwarden".hasHttpProtocol())
    }

    @Test
    fun `hasHttpProtocol should return true when it starts with the http protocol`() {
        assertTrue("http://www.google.com".hasHttpProtocol())
    }

    @Test
    fun `hasHttpProtocol should return true when it starts with the https protocol`() {
        assertTrue("https://www.google.com".hasHttpProtocol())
    }

    @Test
    fun `getWebHostFromAndroidUriOrNull should return null when not android app`() {
        assertNull("com.x8bit.bitwarden".getWebHostFromAndroidUriOrNull())
    }

    @Test
    fun `getWebHostFromAndroidUriOrNull should return null when no dot`() {
        assertNull("androidapp://comx8bitbitwarden".getWebHostFromAndroidUriOrNull())
    }

    @Test
    fun `getWebHostFromAndroidUriOrNull should return web host when has dot`() {
        // Setup
        val expected = "x8bit.com"

        // Test
        val actual = "androidapp://com.x8bit.bitwarden".getWebHostFromAndroidUriOrNull()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getDomainOrNull should invoke parseDomainOrNull when URI is created`() {
        // Setup
        mockkStatic(URI::parseDomainOrNull)
        val resourceCacheManager: ResourceCacheManager = mockk()
        val expected = "google.com"
        every {
            any<URI>().parseDomainOrNull(resourceCacheManager = resourceCacheManager)
        } returns expected

        // Test
        val actual = "www.google.com".getDomainOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            any<URI>().parseDomainOrNull(resourceCacheManager = resourceCacheManager)
        }
    }

    @Test
    fun `getDomainOrNull should not invoke parseDomainOrNull when URI is not created`() {
        // Setup
        mockkStatic(URI::parseDomainOrNull)
        val resourceCacheManager: ResourceCacheManager = mockk()

        // Test
        val actual = "not a URI".getDomainOrNull(
            resourceCacheManager = resourceCacheManager,
        )

        // Verify
        assertNull(actual)
        verify(exactly = 0) {
            any<URI>().parseDomainOrNull(resourceCacheManager = resourceCacheManager)
        }
    }

    @Test
    fun `findLastSubstringIndicesOrNull should return null if substring doesn't appear`() {
        // Setup
        val substring = "hello"

        // Test
        val actual = "goodbye".findLastSubstringIndicesOrNull(
            substring = substring,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `findLastSubstringIndicesOrNull should return indices of last substring appearance`() {
        // Setup
        val substring = "hello"
        val expected = IntRange(7, 11)

        // Test
        val actual = "hello, hello".findLastSubstringIndicesOrNull(
            substring = substring,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `getHostOrNull should return host when one is present`() {
        val expectedHost = "www.google.com"
        assertEquals(expectedHost, expectedHost.getHostOrNull())
    }

    @Test
    fun `getHostOrNull should return null when no host is present`() {
        assertNull("boo".getHostOrNull())
    }

    @Test
    fun `getHostOrNull should return host from URI string when present and custom URI scheme`() {
        val expectedHost = "www.google.com"
        val hostWithScheme = "androidapp://$expectedHost"
        assertEquals(expectedHost, hostWithScheme.getHostOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getHostOrNull should return host from URI string when present and has port but no scheme`() {
        val expectedHost = "www.google.com"
        val hostWithPort = "$expectedHost:8080"
        assertEquals(expectedHost, hostWithPort.getHostOrNull())
    }

    @Test
    fun `hasPort returns true when port is present`() {
        assertTrue("www.google.com:8080".hasPort())
    }

    @Test
    fun `hasPort returns false when port is not present`() {
        assertFalse("www.google.com".hasPort())
    }

    @Test
    fun `hasPort return true when port is present and custom scheme is present`() {
        val uriString = "androidapp://www.google.com:8080"
        assertTrue(uriString.hasPort())
    }

    @Test
    fun `getHostWithPortOrNull should return host with port when present`() {
        val uriString = "www.google.com:8080"
        assertEquals("www.google.com:8080", uriString.getHostWithPortOrNull())
    }

    @Test
    fun `getHostWithPortOrNull should return host when no port is present`() {
        val uriString = "www.google.com"
        assertEquals("www.google.com", uriString.getHostWithPortOrNull())
    }

    @Test
    fun `getHostWithPortOrNull should return null when no host is present`() {
        assertNull("boo".getHostWithPortOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getHostWithPortOrNull should return host with port when present and custom scheme is present`() {
        val uriString = "androidapp://www.google.com:8080"
        assertEquals("www.google.com:8080", uriString.getHostWithPortOrNull())
    }
}
