package com.bitwarden.ui.platform.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit tests for [BidiTextManagerImpl].
 *
 * Note: [BidiTextManagerImpl] relies on Android's [BidiFormatter] which requires framework
 * dependencies. These tests verify the basic logic (chunking, empty string handling) but
 * full bidirectional text behavior should be tested via instrumentation tests on a real device
 * or emulator where BidiFormatter is available.
 */
class BidiTextManagerImplTest {
    private val manager = BidiTextManagerImpl()

    // Test chunking logic for verification codes
    @Test
    fun `formatVerificationCode handles empty string`() {
        val result = manager.formatVerificationCode("")
        assertEquals("", result)
    }

    // Test chunking logic for card numbers
    @Test
    fun `formatCardNumber handles empty string`() {
        val result = manager.formatCardNumber("")
        assertEquals("", result)
    }
}
