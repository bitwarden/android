package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StringExtensionsTest {

    @Test
    fun `validateUri should return null for valid URIs`() {
        val validUri = "https://example.com"
        val existingUris = listOf("http://another.com")

        val result = validUri.validateUri(existingUris)

        assertNull(result)
    }

    @Test
    fun `validateUri should return non-null for URIs with invalid scheme`() {
        val invalidSchemeUri = "ftp://example.com"
        val existingUris = listOf<String>()

        val result = invalidSchemeUri.validateUri(existingUris)

        assertNotNull(result)
    }

    @Test
    fun `validateUri should return non-null for URIs with invalid pattern`() {
        val invalidPatternUri = "https://example..com"
        val existingUris = listOf<String>()

        val result = invalidPatternUri.validateUri(existingUris)

        assertNotNull(result)
    }

    @Test
    fun `validateUri should return non-null for duplicate URIs`() {
        val duplicateUri = "https://example.com"
        val existingUris = listOf("https://example.com")

        val result = duplicateUri.validateUri(existingUris)

        assertNotNull(result)
    }

    @Test
    fun `isValidPattern should correctly validate URIs`() {
        val validUris = listOf(
            "https://a",
            "http://a.com",
            "https://subdomain.example.com",
            "androidapp://com.example.app",
        )

        val invalidUris = listOf(
            "https://a.....",
            "https://a....com",
            "https://.com",
            "ftp://example.com",
        )

        validUris.forEach { uri ->
            assertTrue(uri.isValidPattern(), "Expected valid URI: $uri")
        }

        invalidUris.forEach { uri ->
            assertFalse(uri.isValidPattern(), "Expected invalid URI: $uri")
        }
    }
}
