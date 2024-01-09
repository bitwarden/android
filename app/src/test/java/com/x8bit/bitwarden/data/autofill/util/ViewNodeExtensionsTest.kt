package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.autofill.AutofillId
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ViewNodeExtensionsTest {
    private val expectedAutofillId: AutofillId = mockk()
    private val expectedIsFocused = true
    private val viewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillId } returns expectedAutofillId
        every { this@mockk.childCount } returns 0
        every { this@mockk.isFocused } returns expectedIsFocused
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
        val expected = AutofillView.Card.ExpirationMonth(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationYear when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val expected = AutofillView.Card.ExpirationYear(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card Number when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_NUMBER
        val expected = AutofillView.Card.Number(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card SecurityCode when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
        val expected = AutofillView.Card.SecurityCode(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login EmailAddress when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_EMAIL_ADDRESS
        val expected = AutofillView.Login.EmailAddress(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Identity Name when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_NAME
        val expected = AutofillView.Identity.Name(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Password when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_PASSWORD
        val expected = AutofillView.Login.Password(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Identity PhoneNumber when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_PHONE
        val expected = AutofillView.Identity.PhoneNumber(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Identity PostalAddress when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_POSTAL_ADDRESS
        val expected = AutofillView.Identity.PostalAddress(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Identity PostalCOde when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_POSTAL_CODE
        val expected = AutofillView.Identity.PostalCode(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Username when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_USERNAME
        val expected = AutofillView.Login.Username(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return null when hint is not supported`() {
        // Setup
        val autofillHint = "Shenanigans"
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertNull(actual)
    }

    @Test
    fun `toAutofillView should skip unsupported hint and return supported hint mapping`() {
        // Setup
        val autofillHintOne = "Shenanigans"
        val autofillHintTwo = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val expected = AutofillView.Card.ExpirationYear(
            autofillId = expectedAutofillId,
            isFocused = expectedIsFocused,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHintOne, autofillHintTwo)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }
}
