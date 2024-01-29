package com.x8bit.bitwarden.data.util

import android.content.Context
import com.x8bit.bitwarden.data.platform.util.findLastSubstringIndicesOrNull
import com.x8bit.bitwarden.data.platform.util.getDomainOrNull
import com.x8bit.bitwarden.data.platform.util.getWebHostFromAndroidUriOrNull
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
        val context: Context = mockk()
        val expected = "google.com"
        every {
            any<URI>().parseDomainOrNull(context = context)
        } returns expected

        // Test
        val actual = "www.google.com".getDomainOrNull(
            context = context,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            any<URI>().parseDomainOrNull(context = context)
        }
    }

    @Test
    fun `getDomainOrNull should not invoke parseDomainOrNull when URI is not created`() {
        // Setup
        mockkStatic(URI::parseDomainOrNull)
        val context: Context = mockk()

        // Test
        val actual = "not a URI".getDomainOrNull(
            context = context,
        )

        // Verify
        assertNull(actual)
        verify(exactly = 0) {
            any<URI>().parseDomainOrNull(context = context)
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
}
