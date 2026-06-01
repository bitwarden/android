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
        val htmlInfo: HtmlInfo = mockk {
            every { tag } returns "input"
        }

        val actual = htmlInfo.isInputField

        assertTrue(actual)
    }

    @Test
    fun `isInputField should return false when tag is not 'input'`() {
        val htmlInfo: HtmlInfo = mockk {
            every { tag } returns "not input"
        }

        val actual = htmlInfo.isInputField

        assertFalse(actual)
    }
}
