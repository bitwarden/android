package com.x8bit.bitwarden.data.autofill.util

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
}
