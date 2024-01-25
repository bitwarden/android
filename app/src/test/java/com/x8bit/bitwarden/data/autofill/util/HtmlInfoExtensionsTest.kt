package com.x8bit.bitwarden.data.autofill.util

import android.view.ViewStructure.HtmlInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HtmlInfoExtensionsTest {
    @Test
    fun `isInputField should return true when tag is 'input'`() {
        // Setup
        val htmlInfo: HtmlInfo = mockk {
            every { tag } returns "input"
        }

        // Test
        val actual = htmlInfo.isInputField

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isInputField should return false when tag is not 'input'`() {
        // Setup
        val htmlInfo: HtmlInfo = mockk {
            every { tag } returns "not input"
        }

        // Test
        val actual = htmlInfo.isInputField

        // Verify
        assertFalse(actual)
    }
}
