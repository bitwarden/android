package com.x8bit.bitwarden.data.autofill.util

import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillViewExtensionsTest {
    private val autofillId: AutofillId = mockk()
    private val autofillValue: AutofillValue = mockk()
    private val autofillViewData = AutofillView.Data(
        autofillId = autofillId,
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = false,
        textValue = null,
        hasPasswordTerms = false,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(AutofillValue::forText)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AutofillValue::forText)
    }

    @Test
    fun `buildFilledItemOrNull should return date value when date type`() {
        // Setup
        val rawValue = 2002421451023587L
        val value = rawValue.toString()
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_DATE,
        )
        val autofillView = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forDate(rawValue) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forDate(rawValue)
        }
    }

    @Test
    fun `buildFilledItemOrNull should return null when date type but non-numerical value`() {
        // Setup
        val value = "January 1, 2024"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_DATE,
        )
        val autofillView = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFilledItemOrNull should return null when is expiration month value, list type, and non-numerical value`() {
        // Setup
        val value = "January"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_LIST,
        )
        val autofillView = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = null,
        )

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFilledItemOrNull should return list value when is expiration list value, list type, and 13 options`() {
        // Setup
        val rawValue = 1
        val value = rawValue.toString()
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_LIST,
            autofillOptions = List(13) { it.toString() },
        )
        val autofillView = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = null,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forList(rawValue) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forList(rawValue)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFilledItemOrNull should return list value minus one when is expiration list value, list type, and options size is greater than month value`() {
        // Setup
        val rawValue = 1
        val value = (rawValue).toString()
        val expectedListValue = rawValue - 1
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_LIST,
            autofillOptions = List(12) { it.toString() },
        )
        val autofillView = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = null,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forList(expectedListValue) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forList(expectedListValue)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFilledItemOrNull should return index list value when list type, and value is found in options`() {
        // Setup
        val expectedValue = 1
        val value = "2025"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_LIST,
            autofillOptions = listOf("2024", value, "2026"),
        )
        val autofillView = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forList(expectedValue) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forList(expectedValue)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `buildFilledItemOrNull should return null when list type, and value is not found in options`() {
        // Setup
        val value = "2027"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_LIST,
            autofillOptions = listOf("2024", "2025", "2026"),
        )
        val autofillView = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `buildFilledItemOrNull should return text value when text type`() {
        // Setup
        val value = "Jimmy"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_TEXT,
        )
        val autofillView = AutofillView.Login.Username(
            data = autofillViewData,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forText(value) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forText(value)
        }
    }

    @Test
    fun `buildFilledItemOrNull should return toggle value when toggle type and value is boolean`() {
        // Setup
        val value = "false"
        val expectedValue = value.toBooleanStrict()
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_TOGGLE,
        )
        val autofillView = AutofillView.Login.Username(
            data = autofillViewData,
        )
        val expected = FilledItem(
            autofillId = autofillId,
            value = autofillValue,
        )
        every { AutofillValue.forToggle(expectedValue) } returns autofillValue

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertEquals(expected, actual)
        verify(exactly = 1) {
            AutofillValue.forToggle(expectedValue)
        }
    }

    @Test
    fun `buildFilledItemOrNull should return null when toggle type and value not boolean`() {
        // Setup
        val value = "Jimmy"
        val autofillViewData = autofillViewData.copy(
            autofillType = View.AUTOFILL_TYPE_TOGGLE,
        )
        val autofillView = AutofillView.Login.Username(
            data = autofillViewData,
        )

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertNull(actual)
    }

    @Test
    fun `buildFilledItemOrNull should return null when not recognized type`() {
        // Setup
        val value = "Jimmy"
        val autofillViewData = autofillViewData.copy(
            autofillType = 100,
        )
        val autofillView = AutofillView.Login.Username(
            data = autofillViewData,
        )

        // Test
        val actual = autofillView.buildFilledItemOrNull(
            value = value,
        )

        // Verify
        assertNull(actual)
    }
}
