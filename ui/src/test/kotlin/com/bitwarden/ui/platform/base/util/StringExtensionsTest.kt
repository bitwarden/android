package com.bitwarden.ui.platform.base.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class StringExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `isValidEmail should return appropriate value for possible email addresses when using strict validation`() {
        val invalidEmails = listOf(
            "" to false,
            " " to false,
            "test.com" to false,
            "@" to false,
            "@." to false,
            "@.aa" to false,
            "a@.aa" to false,
            "test@test.com" to true,
            " test@test " to false,
            "test@test.c" to false,
            "a@a.aa" to true,
            "test@test.com" to true,
            "test@test.test.com" to true,
            "test.test@test.com" to true,
            "test.test@test.test.com" to true,
            "test/test@test.test.com" to true,
            "test.test@test/test.com" to false,
            "test*test@test.com" to true,
        )
        invalidEmails.forEach {
            assertEquals(it.first.isValidEmail(), it.second)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isValidEmail should return appropriate value for possible email addresses when not using strict validation`() {
        val invalidEmails = listOf(
            "" to false,
            " " to false,
            "test.com" to false,
            "@" to true,
            "@." to true,
            "@.aa" to true,
            "a@.aa" to true,
            "test@test.com" to true,
            " test@test " to true,
            "test@test.c" to true,
            "a@a.aa" to true,
            "test@test.com" to true,
            "test@test.test.com" to true,
            "test/test@test.test.com" to true,
            "test.test@test/test.com" to true,
            "test*test@test.com" to true,
            "test.test@test.test.com" to true,
        )
        invalidEmails.forEach {
            assertEquals(it.first.isValidEmail(useStrictValidation = false), it.second, "Failed for $it")
        }
    }

    @Test
    fun `isValidUri should return true for an absolute URL`() {
        assertTrue("https://abc.com".isValidUri())
    }

    @Test
    fun `isValidUri should return true for an absolute non-URL path`() {
        assertTrue("file:///abc/com".isValidUri())
    }

    @Test
    fun `isValidUri should return true for a relative URI`() {
        assertTrue("abc.com".isValidUri())
    }

    @Test
    fun `isValidUri should return false for a blank or empty String`() {
        listOf(
            "",
            "  ",
        )
            .forEach { badUri ->
                assertFalse(badUri.isValidUri())
            }
    }

    @Test
    fun `isValidUri should return false when there are invalid characters present`() {
        listOf(
            "abc com",
            "abc<>com",
            "abc[]com",
        )
            .forEach { badUri ->
                assertFalse(badUri.isValidUri())
            }
    }

    @Test
    fun `toHostOrPathOrNull should return the correct value for an absolute URL`() {
        assertEquals(
            "www.abc.com",
            "https://www.abc.com".toHostOrPathOrNull(),
        )
    }

    @Test
    fun `toHostOrPathOrNull should return the correct value for an absolute non-URL path`() {
        assertEquals(
            "/abc/com",
            "file:///abc/com".toHostOrPathOrNull(),
        )
    }

    @Test
    fun `toHostOrPathOrNull should return the correct value for a relative URI`() {
        assertEquals(
            "abc.com",
            "abc.com".toHostOrPathOrNull(),
        )
    }

    @Test
    fun `toHostOrPathOrNull should return null when there are invalid characters present`() {
        listOf(
            "abc com",
            "abc<>com",
            "abc[]com",
        )
            .forEach { badUri ->
                assertNull(badUri.toHostOrPathOrNull())
            }
    }

    @Test
    fun `toHexColorRepresentation should return valid hex color values`() {
        mapOf(
            "First" to "#ff90e20b",
            "Second" to "#ff943060",
            "Multiple words" to "#ffb9d46a",
            "1234567890-=!@#$%^&*()_+[]\\;',./{}|:\"<>?" to "#ff171178",
            "" to "#ff000000",
            " " to "#ff200000",
        )
            .forEach { (input, colorHexOutput) ->
                assertEquals(
                    colorHexOutput,
                    input.toHexColorRepresentation(),
                )
            }
    }

    @Test
    fun `capitalize should return a capitalized string`() {
        val initialString = "lowercase"

        val result = initialString.capitalize()

        assertEquals(
            "Lowercase",
            result,
        )
    }

    @Test
    fun `removeDiacritics should remove diacritics from the string`() {
        val result = "áéíóů".removeDiacritics()
        assertEquals("aeiou", result)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should prefix https when URI is valid and no scheme`() {
        val uri = "example.com"
        val expected = "https://$uri"
        val actual = uri.prefixHttpsIfNecessaryOrNull()

        assertEquals(expected, actual)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return null when URI is empty string`() {
        val uri = ""
        assertNull(uri.prefixHttpsIfNecessaryOrNull())
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return null when URI is whitespace string`() {
        val uri = " "
        assertNull(uri.prefixHttpsIfNecessaryOrNull())
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return null when URI is invalid`() {
        val invalidUri = "invalid uri"
        assertNull(invalidUri.prefixHttpsIfNecessaryOrNull())
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return URI unchanged when scheme is http`() {
        val uri = "http://example.com"
        val actual = uri.prefixHttpsIfNecessaryOrNull()
        assertEquals(uri, actual)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return URI unchanged when scheme is https`() {
        val uri = "https://example.com"
        val actual = uri.prefixHttpsIfNecessaryOrNull()
        assertEquals(uri, actual)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull with long valid URI without scheme`() {
        val uri = "longexamplewithlots.of.subdomains.com"
        val expected = "https://$uri"
        val actual = uri.prefixHttpsIfNecessaryOrNull()
        assertEquals(expected, actual)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should return null when uri contains special characters`() {
        val uri = "example-special!@#$%^&*()_+{}[].com"
        val actual = uri.prefixHttpsIfNecessaryOrNull()
        assertNull(actual)
    }

    @Test
    fun `prefixHttpsIfNecessaryOrNull should prefix URI when it contains numbers and letters`() {
        val uri = "example1234567890.com"
        val expected = "https://$uri"
        val actual = uri.prefixHttpsIfNecessaryOrNull()
        assertEquals(expected, actual)
    }

    @Test
    fun `orNullIfBlank returns null for a null String`() {
        assertNull((null as String?).orNullIfBlank())
    }

    @Test
    fun `orNullIfBlank returns null for an empty String`() {
        assertNull("".orNullIfBlank())
    }

    @Test
    fun `orNullIfBlank returns null for a blank String`() {
        assertNull("      ".orNullIfBlank())
    }

    @Test
    fun `orNullIfBlank returns the original value for a non-blank String`() {
        assertEquals("test", "test".orNullIfBlank())
    }

    @Test
    fun `orNullIfBlank returns the original value for a zero-width space String`() {
        assertEquals("\u200B", "\u200B".orNullIfBlank())
    }

    @Test
    fun `orZeroWidthSpace returns null for a null String`() {
        assertEquals("\u200B", null.orZeroWidthSpace())
    }

    @Test
    fun `orZeroWidthSpace returns zero-width space for an empty String`() {
        assertEquals("\u200B", "".orZeroWidthSpace())
    }

    @Test
    fun `orZeroWidthSpace returns zero-width space for a blank String`() {
        assertEquals("\u200B", "      ".orZeroWidthSpace())
    }

    @Test
    fun `orZeroWidthSpace returns the original value for a non-blank string`() {
        assertEquals("test", "test".orZeroWidthSpace())
    }
}
