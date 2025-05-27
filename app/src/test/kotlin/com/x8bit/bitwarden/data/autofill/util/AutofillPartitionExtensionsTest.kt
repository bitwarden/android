package com.x8bit.bitwarden.data.autofill.util

import android.view.View
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AutofillPartitionExtensionsTest {
    private val autofillDataEmptyText: AutofillView.Data = AutofillView.Data(
        autofillId = mockk(),
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = false,
        textValue = null,
        hasPasswordTerms = false,
    )
    private val autofillDataValidText: AutofillView.Data = AutofillView.Data(
        autofillId = mockk(),
        autofillOptions = emptyList(),
        autofillType = View.AUTOFILL_TYPE_TEXT,
        isFocused = false,
        textValue = TEXT_VALUE,
        hasPasswordTerms = false,
    )

    @Test
    fun `expirationMonthSaveValue should return null when no month views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.Number(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationMonthSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `expirationMonthSaveValue should return null when has month view but no monthValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationMonth(
                    data = autofillDataValidText,
                    monthValue = null,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationMonthSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `expirationMonthSaveValue should return text value when has month view has monthValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationMonth(
                    data = autofillDataValidText,
                    monthValue = TEXT_VALUE,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationMonthSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `expirationYearSaveValue should return null when no year views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.Number(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationYearSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `expirationYearSaveValue should return null when has year view but no textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationYear(
                    data = autofillDataEmptyText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationYearSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `expirationYearSaveValue should return text value when has year view has textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationYear(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.expirationYearSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `numberSaveValue should return null when no number views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationYear(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.numberSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `numberSaveValue should return null when has number view but no textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.Number(
                    data = autofillDataEmptyText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.numberSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `numberSaveValue should return text value when has number view has textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.Number(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.numberSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `securityCodeSaveValue should return null when no code views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.ExpirationYear(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.securityCodeSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `securityCodeSaveValue should return null when has code view but no textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.SecurityCode(
                    data = autofillDataEmptyText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.securityCodeSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `securityCodeSaveValue should return text value when has code view has textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                AutofillView.Card.SecurityCode(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.securityCodeSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `passwordSaveValue should return null when no password views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Username(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.passwordSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `passwordSaveValue should return null when has password view but no textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Password(
                    data = autofillDataEmptyText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.passwordSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `passwordSaveValue should return text value when has password view has textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Password(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.passwordSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }

    @Test
    fun `usernameSaveValue should return null when no username views present`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Password(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.usernameSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `usernameSaveValue should return null when has username view but no textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Username(
                    data = autofillDataEmptyText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.passwordSaveValue

        // Verify
        assertNull(actual)
    }

    @Test
    fun `usernameSaveValue should return text value when has username view has textValue`() {
        // Setup
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                AutofillView.Login.Username(
                    data = autofillDataValidText,
                ),
            ),
        )

        // Test
        val actual = autofillPartition.usernameSaveValue

        // Verify
        assertEquals(TEXT_VALUE, actual)
    }
}

private const val TEXT_VALUE: String = "TEXT_VALUE"
