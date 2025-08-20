package com.x8bit.bitwarden.data.autofill.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StringExtensionsTest {
    @Test
    fun `containsAnyTerms returns true when string contains a term`() {
        // Setup
        val terms = listOf(
            "bike",
            "bicycle",
        )
        val string = "I want to ride my bicycle"

        // Test
        val actual = string.containsAnyTerms(
            terms = terms,
            ignoreCase = false,
        )

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `containsAnyTerms returns false when string doesn't contain a term`() {
        // Setup
        val terms = listOf(
            "bike",
            "bicycle",
        )
        val string = "I want to ride my tricycle"

        // Test
        val actual = string.containsAnyTerms(
            terms = terms,
            ignoreCase = false,
        )

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `containsAnyTerms returns true when string contains a term while ignoring case`() {
        // Setup
        val terms = listOf(
            "bike",
            "bicycle",
        )
        val string = "I want to ride my BICYCLE"

        // Test
        val actual = string.containsAnyTerms(
            terms = terms,
            ignoreCase = true,
        )

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `matchesAnyExpressions returns true when string matches an expression`() {
        val patterns = listOf(
            Regex(".*bike.*"),
            Regex(".*bicycle.*"),
        )
        val string = "I want to ride my bicycle"
        val actual = string.matchesAnyExpressions(patterns)
        assertTrue(actual)
    }

    @Test
    fun `matchesAnyExpressions returns false when string doesn't match an expression`() {
        val patterns = listOf(
            Regex(".*bike.*"),
            Regex(".*bicycle.*"),
        )
        val string = "I want to ride my tricycle"
        val actual = string.matchesAnyExpressions(patterns)
        assertFalse(actual)
    }

    @Test
    fun `toLowerCaseAndStripNonAlpha returns lowercase string with non-alpha characters removed`() {
        val string = "Hello, World!"
        val actual = string.toLowerCaseAndStripNonAlpha()
        assertEquals("helloworld", actual)
    }

    @Test
    fun `toLowerCaseAndStripNonAlpha returns empty string when input is empty`() {
        val string = ""
        val actual = string.toLowerCaseAndStripNonAlpha()
        assertEquals("", actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `toLowerCaseAndStripNonAlpha returns empty string when input contains only non-alpha characters`() {
        val string = "1234567890"
        val actual = string.toLowerCaseAndStripNonAlpha()
        assertEquals("", actual)
    }
}
