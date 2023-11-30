package com.x8bit.bitwarden.data.platform.base.util

import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.base.util.orZeroWidthSpace
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
