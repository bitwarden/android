package com.x8bit.bitwarden.data.platform.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StringExtensionsTest {
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
        assertEquals(
            "test",
            "test".orNullIfBlank(),
        )
    }
}
