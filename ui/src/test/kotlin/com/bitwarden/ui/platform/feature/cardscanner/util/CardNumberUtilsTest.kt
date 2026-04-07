package com.bitwarden.ui.platform.feature.cardscanner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardNumberUtilsTest {

    @Test
    fun `sanitizeCardNumber should remove non-digit characters`() {
        assertEquals(
            "4111111111111111",
            "4111 1111 1111 1111".sanitizeCardNumber(),
        )
        assertEquals(
            "4111111111111111",
            "4111-1111-1111-1111".sanitizeCardNumber(),
        )
        assertEquals(
            "4111111111111111",
            "4111111111111111".sanitizeCardNumber(),
        )
    }

    @Test
    fun `isValidLuhn should return true for valid card numbers`() {
        assertTrue("4111111111111111".isValidLuhn())
        assertTrue("5500000000000004".isValidLuhn())
        assertTrue("378282246310005".isValidLuhn())
        assertTrue("6011111111111117".isValidLuhn())
    }

    @Test
    fun `isValidLuhn should return false for invalid card numbers`() {
        assertFalse("4111111111111112".isValidLuhn())
        assertFalse("1234567890".isValidLuhn())
        assertFalse("".isValidLuhn())
        assertFalse("12345".isValidLuhn())
    }

    @Test
    fun `isValidLuhn should handle formatted numbers`() {
        assertTrue("4111 1111 1111 1111".isValidLuhn())
        assertTrue("4111-1111-1111-1111".isValidLuhn())
    }
}
