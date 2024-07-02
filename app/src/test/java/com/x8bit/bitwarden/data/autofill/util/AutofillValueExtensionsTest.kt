package com.x8bit.bitwarden.data.autofill.util

import android.view.autofill.AutofillValue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AutofillValueExtensionsTest {
    @Test
    fun `extractMonthValue should return listValue when isList and options size 13`() {
        // Setup
        val autofillOptions = List(13) { "option-$it" }
        val autofillValue: AutofillValue = mockk {
            every { isList } returns true
            every { listValue } returns LIST_VALUE
        }
        val expected = LIST_VALUE.toString()

        // Test
        val actual = autofillValue.extractMonthValue(autofillOptions)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `extractMonthValue should return listValue plus one when isList and options size 12`() {
        // Setup
        val autofillOptions = List(12) { "option-$it" }
        val autofillValue: AutofillValue = mockk {
            every { isList } returns true
            every { listValue } returns LIST_VALUE
        }
        val expected = (LIST_VALUE + 1).toString()

        // Test
        val actual = autofillValue.extractMonthValue(autofillOptions)

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `extractMonthValue should return textValue when isText`() {
        // Setup
        val autofillOptions = List(1) { "option-$it" }
        val autofillValue: AutofillValue = mockk {
            every { isList } returns false
            every { isText } returns true
            every { textValue } returns TEXT_VALUE
        }

        // Test
        val actual = autofillValue.extractMonthValue(autofillOptions)

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `extractMonthValue should return null not list or text`() {
        // Setup
        val autofillOptions = List(1) { "option-$it" }
        val autofillValue: AutofillValue = mockk {
            every { isList } returns false
            every { isText } returns false
        }

        // Test
        val actual = autofillValue.extractMonthValue(autofillOptions)

        // Verify
        assertNull(actual)
    }

    @Test
    fun `extractTextValue should return textValue when not blank`() {
        // Setup
        val autofillValue: AutofillValue = mockk {
            every { isText } returns true
            every { textValue } returns TEXT_VALUE
        }

        // Test
        val actual = autofillValue.extractTextValue()

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `extractTextValue should return null when not blank`() {
        // Setup
        val autofillValue: AutofillValue = mockk {
            every { isText } returns true
            every { textValue } returns "   "
        }

        // Test
        val actual = autofillValue.extractTextValue()

        // Verify
        assertNull(actual)
    }

    @Test
    fun `extractTextValue should return null when not text`() {
        // Setup
        val autofillValue: AutofillValue = mockk {
            every { isText } returns false
        }

        // Test
        val actual = autofillValue.extractTextValue()

        // Verify
        assertNull(actual)
    }
}

private const val LIST_VALUE: Int = 5
private const val TEXT_VALUE: String = "TEXT_VALUE"
