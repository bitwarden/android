package com.x8bit.bitwarden.data.autofill.util

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillViewExtensionsTest {
    private val autofillId: AutofillId = mockk()
    private val autofillValue: AutofillValue = mockk()
    private val autofillViewData = AutofillView.Data(
        autofillId = autofillId,
        isFocused = false,
        textValue = null,
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
    fun `buildFilledItem returns AutofillValue`() {
        // Setup
        val value = "2002421451023587L"
        val autofillView = AutofillView.Card.Number(
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
}
