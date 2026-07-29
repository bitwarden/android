package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.ViewStructure.HtmlInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.autofill.HintConstants
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("LargeClass")
class ViewNodeExtensionsTest {
    private val expectedAutofillId: AutofillId = mockk()
    private val expectedIsFocused = true
    private val autofillViewData = AutofillView.Data(
        autofillId = expectedAutofillId,
        autofillOptions = AUTOFILL_OPTIONS_LIST,
        autofillType = AUTOFILL_TYPE,
        isFocused = expectedIsFocused,
        textValue = TEXT_VALUE,
        hasPasswordTerms = false,
        website = null,
    )
    private val testAutofillValue: AutofillValue = mockk()
    private val mockHtmlInfo: HtmlInfo = mockk {
        every { attributes } returns emptyList()
        every { tag } returns ""
    }

    private val viewNode: AssistStructure.ViewNode = mockk {
        every { autofillId } returns expectedAutofillId
        every { className } returns null
        every { idEntry } returns null
        every { hint } returns null
        every { autofillOptions } returns AUTOFILL_OPTIONS_ARRAY
        every { autofillType } returns AUTOFILL_TYPE
        every { autofillValue } returns testAutofillValue
        every { childCount } returns 0
        every { inputType } returns 1
        every { isFocused } returns expectedIsFocused
        every { htmlInfo } returns mockHtmlInfo
        every { website } returns null
    }

    @BeforeEach
    fun setup() {
        mockkStatic(AssistStructure.ViewNode::website)
        mockkStatic(HtmlInfo::isInputField)
        mockkStatic(HtmlInfo::isPasswordField)
        mockkStatic(Int::isPasswordInputType)
        mockkStatic(Int::isUsernameInputType)
        mockkStatic(AutofillValue::extractMonthValue)
        mockkStatic(AutofillValue::extractYearValue)
        mockkStatic(AutofillValue::extractTextValue)
        every {
            testAutofillValue.extractMonthValue(
                autofillOptions = AUTOFILL_OPTIONS_LIST,
            )
        } returns MONTH_VALUE
        every {
            testAutofillValue.extractYearValue(
                autofillOptions = AUTOFILL_OPTIONS_LIST,
            )
        } returns YEAR_VALUE
        every { testAutofillValue.extractTextValue() } returns TEXT_VALUE
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AssistStructure.ViewNode::website)
        unmockkStatic(HtmlInfo::isInputField)
        unmockkStatic(HtmlInfo::isPasswordField)
        unmockkStatic(Int::isPasswordInputType)
        unmockkStatic(Int::isUsernameInputType)
        unmockkStatic(AutofillValue::extractMonthValue)
        unmockkStatic(AutofillValue::extractYearValue)
        unmockkStatic(AutofillValue::extractTextValue)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when autofillHints match`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = MONTH_VALUE,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth with empty options when autofillHints match and options are null`() {
        // Setup
        val autofillViewData = autofillViewData.copy(
            autofillOptions = emptyList(),
        )
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
        val monthValue = "11"
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = monthValue,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)
        every { viewNode.autofillOptions } returns null
        every {
            testAutofillValue.extractMonthValue(
                autofillOptions = emptyList(),
            )
        } returns monthValue

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when html info isCardExpirationMonthField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = MONTH_VALUE,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARD_EXP_MONTH_HINTS

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when idEntry matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = MONTH_VALUE,
        )
        SUPPORTED_RAW_CARD_EXP_MONTH_HINTS.forEach { idEntry ->
            every { viewNode.idEntry } returns idEntry

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for idEntry: $idEntry")
        }
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
            monthValue = MONTH_VALUE,
        )
        SUPPORTED_RAW_CARD_EXP_MONTH_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint
            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )
            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationYear when autofillHints match`() {
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val expected = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
            yearValue = YEAR_VALUE,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationYear when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
            yearValue = YEAR_VALUE,
        )
        SUPPORTED_RAW_CARD_EXP_YEAR_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationYear when html info isCardExpirationYearField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
            yearValue = YEAR_VALUE,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARD_EXP_YEAR_HINTS

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationDate when autofillHints match`() {
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE
        val expected = AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)
        every { mockHtmlInfo.isInputField } returns true

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationDate when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
        SUPPORTED_RAW_CARD_EXP_DATE_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card ExpirationDate when html info isCardExpirationDateField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.ExpirationDate(
            data = autofillViewData,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARD_EXP_DATE_HINTS

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card Number when autofillHints match`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_NUMBER
        val expected = AutofillView.Card.Number(
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card Number when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.Number(
            data = autofillViewData,
        )
        SUPPORTED_RAW_CARD_NUMBER_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            // Flag off pins the identity gate below the card heuristics.
            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = false,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Test
    fun `toAutofillView should return AutofillView Card Number when html info isCardNumberField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.Number(
            data = autofillViewData,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARD_NUMBER_HINTS

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card SecurityCode when autofillHints match`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE
        val expected = AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card SecurityCode when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
        SUPPORTED_RAW_CARD_SECURITY_CODE_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card SecurityCode when html info isCardSecurityCodeField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARD_SECURITY_CODE_HINTS
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Card CardholderName when idEntry matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
        SUPPORTED_RAW_CARDHOLDER_NAME_HINTS.forEach { idEntry ->
            every { viewNode.idEntry } returns idEntry

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for idEntry: $idEntry")
        }
    }

    @Test
    fun `toAutofillView should return AutofillView Card CardholderName when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
        SUPPORTED_RAW_CARDHOLDER_NAME_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Card CardholderName when html info isCardholderNameField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Card.CardholderName(
            data = autofillViewData,
        )
        every { viewNode.htmlInfo.hints() } returns SUPPORTED_RAW_CARDHOLDER_NAME_HINTS
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Login Password when autofillHints match`() {
        val autofillHint = View.AUTOFILL_HINT_PASSWORD
        val expected = AutofillView.Login.Password(
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Password when hint matches`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Login.Password(
            data = autofillViewData.copy(hasPasswordTerms = true),
        )
        SUPPORTED_RAW_PASSWORD_HINTS.forEach { hint ->
            every { viewNode.hint } returns hint

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "Failed for hint: $hint")
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Login Password when html info isPasswordField`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Login.Password(
            data = autofillViewData,
        )
        every { viewNode.htmlInfo.isPasswordField() } returns true

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Username with internal website`() {
        // Setup
        val website = "website"
        val expected = AutofillView.Login.Username(
            data = autofillViewData.copy(website = website),
        )
        setupUnsupportedInputFieldViewNode()
        every { viewNode.website } returns website
        every { viewNode.className } returns "android.widget.EditText"
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns true

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Username with external website`() {
        // Setup
        val website = "website"
        val expected = AutofillView.Login.Username(
            data = autofillViewData.copy(website = website),
        )
        setupUnsupportedInputFieldViewNode()
        every { viewNode.className } returns "android.widget.EditText"
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns true

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = website,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Login Username when is EditText and isUsernameField`() {
        // Setup
        val expected = AutofillView.Login.Username(
            data = autofillViewData,
        )
        setupUnsupportedInputFieldViewNode()
        every { viewNode.className } returns "android.widget.EditText"
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns true

        // Test
        // Flag off pins the identity gate below the username heuristic.
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = false,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Login Username when is EditText subclass and isUsernameField`() {
        // Setup
        val expected = AutofillView.Login.Username(
            data = autofillViewData,
        )
        setupUnsupportedInputFieldViewNode()
        every { viewNode.className } returns "android.widget.AutoCompleteTextView"
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns true

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return null when hint is not supported and isn't an inputField`() {
        // Setup
        val autofillHint = "Shenanigans"
        every { viewNode.autofillHints } returns arrayOf(autofillHint)
        every { viewNode.className } returns null
        every { viewNode.htmlInfo.isInputField } returns false

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return only unused field when hint is not supported, is an inputField, and isn't a username or password`() {
        setupUnsupportedInputFieldViewNode()
        val expected = AutofillView.Unused(
            data = autofillViewData,
        )

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should skip unsupported hint and return supported hint mapping`() {
        // Setup
        val autofillHintOne = "Shenanigans"
        val autofillHintTwo = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR
        val expected = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
            yearValue = YEAR_VALUE,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHintOne, autofillHintTwo)

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `isPasswordField returns true when html hints contains a supported password hint`() {
        // Setup
        every { mockHtmlInfo.isInputField } returns true
        every { mockHtmlInfo.hints() } returns listOf("password")

        // Test
        val actual = viewNode.isPasswordField

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordField returns true when hints aren't supported, isPasswordInputType, isValidField, and isn't username`() {
        // Setup
        setupUnsupportedInputFieldViewNode()
        every { any<Int>().isPasswordInputType } returns true

        // Test
        val actual = viewNode.isPasswordField

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordField returns true when hints aren't supported, not isPasswordInputType, and htmlInfo isPasswordField is true`() {
        // Setup
        setupUnsupportedInputFieldViewNode()
        every { viewNode.htmlInfo.isPasswordField() } returns true

        // Test
        val actual = viewNode.isPasswordField

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordField returns false when hints aren't supported, isPasswordInputType, not validInputField, and htmlInfo isPasswordField is false`() {
        // Setup testing the hint
        setupUnsupportedInputFieldViewNode()
        every { any<Int>().isPasswordInputType } returns true

        IGNORED_RAW_HINTS.forEach { hint ->
            // Setup
            every { viewNode.hint } returns hint

            // Test
            val actual = viewNode.isPasswordField

            // Verify
            assertFalse(actual)
        }

        // Setup testing the idEntry
        every { viewNode.hint } returns null
        IGNORED_RAW_HINTS.forEach { hint ->
            // Setup
            every { viewNode.idEntry } returns hint

            // Test
            val actual = viewNode.isPasswordField

            // Verify
            assertFalse(actual)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordField returns false when hints aren't supported, isPasswordInputType, validInputField, isUsernameField, and htmlInfo isPasswordField is false`() {
        // Setup
        setupUnsupportedInputFieldViewNode()
        every { any<Int>().isPasswordInputType } returns true
        every { viewNode.hint } returns SUPPORTED_RAW_USERNAME_HINTS.first()

        // Test
        val actual = viewNode.isPasswordField

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isUsernameField returns true when html hints contains a supported username hint`() {
        // Setup
        every { mockHtmlInfo.isInputField } returns true
        every { mockHtmlInfo.hints() } returns SUPPORTED_RAW_USERNAME_HINTS

        // Test
        val actual = viewNode.isUsernameField

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isUsernameField returns true when supportedHint is null and raw hint is supported`() {
        // Setup testing the hints
        every { viewNode.idEntry } returns null
        SUPPORTED_RAW_USERNAME_HINTS.forEach { hint ->
            // Setup
            every { viewNode.hint } returns hint

            // Test
            val actual = viewNode.isUsernameField

            // Verify
            assertTrue(actual)
        }

        // Setup testing the idEntries
        every { viewNode.hint } returns null
        SUPPORTED_RAW_USERNAME_HINTS.forEach { hint ->
            // Setup
            every { viewNode.idEntry } returns hint

            // Test
            val actual = viewNode.isUsernameField

            // Verify
            assertTrue(actual)
        }
    }

    @Test
    fun `isUsernameField returns true when htmlInfo isUserNameField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isUsernameField() } returns true

        // Test
        val actual = viewNode.isUsernameField

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `hasPasswordTerms returns true when idEntry contains a raw password term`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null

        // Test
        val actual = viewNode.hasPasswordTerms()

        // Verify
        assertFalse(actual)

        SUPPORTED_RAW_PASSWORD_HINTS.map {
            every { viewNode.idEntry } returns it

            // Test
            val actual = viewNode.hasPasswordTerms()

            // Verify
            assertTrue(actual)
        }
    }

    @Test
    fun `hasPasswordTerms returns true when hint contains a raw password term`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null

        // Test
        val actual = viewNode.hasPasswordTerms()

        // Verify
        assertFalse(actual)

        SUPPORTED_RAW_PASSWORD_HINTS.map {
            every { viewNode.hint } returns it

            // Test
            val actual = viewNode.hasPasswordTerms()

            // Verify
            assertTrue(actual)
        }
    }

    @Test
    fun `isCardholderNameField returns true when htmlInfo isCardholderNameField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardholderNameField() } returns true

        val actual = viewNode.isCardholderNameField

        assertTrue(actual)
    }

    @Test
    fun `isCardholderNameField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardholderNameField() } returns false

        SUPPORTED_RAW_CARDHOLDER_NAME_HINTS.forEach {
            every { viewNode.hint } returns it

            val actual = viewNode.isCardholderNameField

            assertTrue(actual) { "Failed for hint: $it" }
        }
    }

    @Test
    fun `isCardholderNameField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardholderNameField() } returns false

        SUPPORTED_RAW_CARDHOLDER_NAME_HINTS.forEach {
            every { viewNode.idEntry } returns it

            val actual = viewNode.isCardholderNameField

            assertTrue(actual) { "Failed for idEntry: $it" }
        }
    }

    @Test
    fun `isCardholderNameField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardholderNameField() } returns false

        val actual = viewNode.isCardholderNameField

        assertFalse(actual)
    }

    @Test
    fun `isCardholderNameField returns false when idEntry, hint, and htmlInfo are not supported`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardholderNameField() } returns false

        val actual = viewNode.isCardholderNameField

        assertFalse(actual)
    }

    @Test
    fun `isCardBrandField returns true when htmlInfo isCardBrandField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardBrandField() } returns true

        val actual = viewNode.isCardBrandField

        assertTrue(actual)
    }

    @Test
    fun `isCardBrandField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardBrandField() } returns false

        SUPPORTED_RAW_CARD_BRAND_HINTS.forEach {
            every { viewNode.hint } returns it

            val actual = viewNode.isCardBrandField

            assertTrue(actual) { "Failed for hint: $it" }
        }
    }

    @Test
    fun `isCardBrandField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardBrandField() } returns false

        SUPPORTED_RAW_CARD_BRAND_HINTS.forEach {
            every { viewNode.idEntry } returns it

            val actual = viewNode.isCardBrandField

            assertTrue(actual) { "Failed for idEntry: $it" }
        }
    }

    @Test
    fun `isCardBrandField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardBrandField() } returns false

        val actual = viewNode.isCardBrandField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardBrandField returns false when idEntry and hint are not supported, and htmlInfo isCardBrandField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardBrandField() } returns false

        val actual = viewNode.isCardBrandField

        assertFalse(actual)
    }

    @Test
    fun `isCardNumberField returns true when htmlInfo isCardNumberField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardNumberField() } returns true

        val actual = viewNode.isCardNumberField

        assertTrue(actual)
    }

    @Test
    fun `isCardNumberField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardNumberField() } returns false
        every { viewNode.hint } returns "credit-card-number"

        val actual = viewNode.isCardNumberField

        assertTrue(actual)
    }

    @Test
    fun `isCardNumberField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardNumberField() } returns false
        every { viewNode.idEntry } returns "credit-card-number"

        val actual = viewNode.isCardNumberField

        assertTrue(actual)
    }

    @Test
    fun `isCardNumberField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardNumberField() } returns false

        val actual = viewNode.isCardNumberField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardNumberField returns false when idEntry and hint are not supported, and htmlInfo isCardNumberField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardNumberField() } returns false

        val actual = viewNode.isCardNumberField

        assertFalse(actual)
    }

    @Test
    fun `isCardSecurityCodeField returns true when htmlInfo isCardSecurityCodeField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardSecurityCodeField() } returns true

        val actual = viewNode.isCardSecurityCodeField

        assertTrue(actual)
    }

    @Test
    fun `isCardSecurityCodeField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardSecurityCodeField() } returns false

        SUPPORTED_RAW_CARD_SECURITY_CODE_HINTS.forEach {
            every { viewNode.hint } returns it

            val actual = viewNode.isCardSecurityCodeField

            assertTrue(actual) { "Failed for hint: $it" }
        }
    }

    @Test
    fun `isCardSecurityCodeField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardSecurityCodeField() } returns false

        SUPPORTED_RAW_CARD_SECURITY_CODE_HINTS.forEach {
            every { viewNode.idEntry } returns it

            val actual = viewNode.isCardSecurityCodeField

            assertTrue(actual) { "Failed for idEntry: $it" }
        }
    }

    @Test
    fun `isCardSecurityCodeField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardSecurityCodeField() } returns false

        val actual = viewNode.isCardSecurityCodeField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardSecurityCodeField returns false when idEntry and hint are not supported, and htmlInfo isCardSecurityCodeField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardSecurityCodeField() } returns false

        val actual = viewNode.isCardSecurityCodeField

        assertFalse(actual)
    }

    @Test
    fun `isCardExpirationDateField returns true when htmlInfo isCardExpirationDateField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationDateField() } returns true

        // Test
        val actual = viewNode.isCardExpirationDateField

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationDateField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardExpirationDateField() } returns false
        every { viewNode.hint } returns "expiration_date"

        val actual = viewNode.isCardExpirationDateField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationDateField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationDateField() } returns false
        every { viewNode.idEntry } returns "expiration_date"

        val actual = viewNode.isCardExpirationDateField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationDateField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationDateField() } returns false

        val actual = viewNode.isCardExpirationDateField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardExpirationDateField returns false when idEntry and hint are not supported, and htmlInfo isCardExpirationDateField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardExpirationDateField() } returns false

        val actual = viewNode.isCardExpirationDateField

        assertFalse(actual)
    }

    @Test
    fun `isCardExpirationYearField returns true when htmlInfo isCardExpirationYearField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationYearField() } returns true

        // Test
        val actual = viewNode.isCardExpirationYearField

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationYearField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardExpirationYearField() } returns false
        every { viewNode.hint } returns "expiration_year"

        val actual = viewNode.isCardExpirationYearField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationYearField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationYearField() } returns false
        every { viewNode.idEntry } returns "expiration_year"

        val actual = viewNode.isCardExpirationYearField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationYearField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationYearField() } returns false

        val actual = viewNode.isCardExpirationYearField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardExpirationYearField returns false when idEntry and hint are not supported and htmlInfo isCardExpirationYearField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardExpirationYearField() } returns false

        val actual = viewNode.isCardExpirationYearField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardExpirationMonthField returns true when htmlInfo isCardExpirationMonthField is true`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationMonthField() } returns true

        // Test
        val actual = viewNode.isCardExpirationMonthField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationMonthField returns true when hint is supported`() {
        every { viewNode.idEntry } returns null
        every { viewNode.htmlInfo.isCardExpirationMonthField() } returns false
        every { viewNode.hint } returns "expiration_month"

        val actual = viewNode.isCardExpirationMonthField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationMonthField returns true when idEntry is supported`() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationMonthField() } returns false
        every { viewNode.idEntry } returns "expiration_month"

        val actual = viewNode.isCardExpirationMonthField

        assertTrue(actual)
    }

    @Test
    fun `isCardExpirationMonthField returns false when idEntry, hint, and htmlInfo are all null`() {
        every { viewNode.idEntry } returns null
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isCardExpirationMonthField() } returns false

        val actual = viewNode.isCardExpirationMonthField

        assertFalse(actual)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `isCardExpirationMonthField returns false when idEntry and hint are not supported, and htmlInfo isCardExpirationMonthField is false`() {
        every { viewNode.idEntry } returns "unsupportedIdEntry"
        every { viewNode.hint } returns "unsupportedHint"
        every { viewNode.htmlInfo.isCardExpirationMonthField() } returns false

        val actual = viewNode.isCardExpirationMonthField

        assertFalse(actual)
    }

    @Test
    fun `website should return URI if domain and scheme are valid`() {
        // Setup
        val webDomain = "www.google.com"
        val webScheme = "http"
        val expected = "http://www.google.com"
        every { viewNode.webDomain } returns webDomain
        every { viewNode.webScheme } returns webScheme

        // Test
        val actual = viewNode.website

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `website should return URI with default scheme if domain is valid and scheme is null`() {
        // Setup
        val webDomain = "www.google.com"
        val webScheme = null
        val expected = "https://www.google.com"
        every { viewNode.webDomain } returns webDomain
        every { viewNode.webScheme } returns webScheme

        // Test
        val actual = viewNode.website

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `website should return URI with default scheme if domain is valid and scheme is blank`() {
        // Setup
        val webDomain = "www.google.com"
        val webScheme = " "
        val expected = "https://www.google.com"
        every { viewNode.webDomain } returns webDomain
        every { viewNode.webScheme } returns webScheme

        // Test
        val actual = viewNode.website

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `website should return null when domain is null`() {
        // Setup
        val webDomain = null
        every { viewNode.webDomain } returns webDomain

        // Test
        val actual = viewNode.website

        // Verify
        assertNull(actual)
    }

    @Test
    fun `website should return null when domain is blank`() {
        // Setup
        val webDomain = " "
        every { viewNode.webDomain } returns webDomain

        // Test
        val actual = viewNode.website

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should redirect autofillId and autofillType to first autofillable child when container has AUTOFILL_TYPE_NONE and a supported hint`() {
        // Setup
        val childAutofillId: AutofillId = mockk()
        val childAutofillType = View.AUTOFILL_TYPE_TEXT
        val childNode: AssistStructure.ViewNode = mockk {
            every { autofillId } returns childAutofillId
            every { autofillType } returns childAutofillType
            every { childCount } returns 0
            every { className } returns null
            every { htmlInfo } returns mockHtmlInfo
        }
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_PASSWORD)
        every { viewNode.autofillType } returns View.AUTOFILL_TYPE_NONE
        every { viewNode.childCount } returns 1
        every { viewNode.getChildAt(0) } returns childNode
        val expected = AutofillView.Login.Password(
            data = autofillViewData.copy(
                autofillId = childAutofillId,
                autofillType = childAutofillType,
            ),
        )

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should redirect to nested grandchild when direct child has AUTOFILL_TYPE_NONE`() {
        // Setup
        val grandchildAutofillId: AutofillId = mockk()
        val grandchildAutofillType = View.AUTOFILL_TYPE_TEXT
        val grandchildNode: AssistStructure.ViewNode = mockk {
            every { autofillId } returns grandchildAutofillId
            every { autofillType } returns grandchildAutofillType
            every { childCount } returns 0
            every { className } returns null
            every { htmlInfo } returns mockHtmlInfo
        }
        val intermediateChildNode: AssistStructure.ViewNode = mockk {
            every { autofillId } returns mockk()
            every { autofillType } returns View.AUTOFILL_TYPE_NONE
            every { childCount } returns 1
            every { getChildAt(0) } returns grandchildNode
            every { className } returns null
            every { htmlInfo } returns mockHtmlInfo
        }
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_PASSWORD)
        every { viewNode.autofillType } returns View.AUTOFILL_TYPE_NONE
        every { viewNode.childCount } returns 1
        every { viewNode.getChildAt(0) } returns intermediateChildNode
        val expected = AutofillView.Login.Password(
            data = autofillViewData.copy(
                autofillId = grandchildAutofillId,
                autofillType = grandchildAutofillType,
            ),
        )

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should fall back to container own autofillId when AUTOFILL_TYPE_NONE and hint present but no autofillable child exists`() {
        // Setup
        val nonAutofillableChildNode: AssistStructure.ViewNode = mockk {
            every { autofillId } returns mockk()
            every { autofillType } returns View.AUTOFILL_TYPE_NONE
            every { childCount } returns 0
            every { className } returns null
            every { htmlInfo } returns mockHtmlInfo
        }
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_PASSWORD)
        every { viewNode.autofillType } returns View.AUTOFILL_TYPE_NONE
        every { viewNode.childCount } returns 1
        every { viewNode.getChildAt(0) } returns nonAutofillableChildNode
        val expected = AutofillView.Login.Password(
            data = autofillViewData.copy(
                autofillType = View.AUTOFILL_TYPE_NONE,
            ),
        )

        // Test
        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        // Verify
        assertEquals(expected, actual)
    }

    //region Identity: official autofillHints dispatch (toAutofillView)

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PersonNameFull when autofillHints match`() {
        every { viewNode.autofillHints } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PersonNameFull(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PersonNamePrefix when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME_PREFIX)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PersonNamePrefix(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PersonNameGiven when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PersonNameGiven(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PersonNameMiddle when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME_MIDDLE)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PersonNameMiddle(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PersonNameFamily when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME_FAMILY)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PersonNameFamily(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity PostalAddressFull when autofillHints match`() {
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_POSTAL_ADDRESS)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.PostalAddressFull(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressStreet when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_STREET_ADDRESS)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressStreet(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressStreet when html info hint equals address exactly`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.htmlInfo.hints() } returns listOf("address")

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressStreet(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should not treat an ADDRESS_HOME_CITY html hint as street when address is exact-matched`() {
        setupUnsupportedInputFieldViewNode()
        // Regression guard: the exact "address" street term must not swallow other address
        // subfields, whose browser-computed hints all contain the substring "address".
        every { viewNode.htmlInfo.hints() } returns listOf("ADDRESS_HOME_CITY")

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressLocality(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressExtended when address-line2 autofillHint matches`() {
        every { viewNode.autofillHints } returns arrayOf("address-line2")

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressExtended(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressExtended when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_EXTENDED_ADDRESS)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressExtended(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressExtended when apt number autofillHint matches`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_APT_NUMBER)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressExtended(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressLocality when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_LOCALITY)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressLocality(data = autofillViewData),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressRegion when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_REGION)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(AutofillView.Identity.AddressRegion(data = autofillViewData), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Identity AddressCountry when autofillHints match`() {
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_POSTAL_ADDRESS_COUNTRY)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(
            AutofillView.Identity.AddressCountry(data = autofillViewData),
            actual,
        )
    }

    @Test
    fun `toAutofillView should return AutofillView Identity PostalCode when autofillHints match`() {
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_POSTAL_CODE)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(AutofillView.Identity.PostalCode(data = autofillViewData), actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Identity PhoneFull when autofillHints match`() {
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_PHONE)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = true,
        )

        assertEquals(AutofillView.Identity.PhoneFull(data = autofillViewData), actual)
    }

    //endregion Identity: official autofillHints dispatch (toAutofillView)

    @Test
    fun `toAutofillView should map every identity heuristic to its Identity view`() {
        // Each idEntry uses a different term from the isXxxField tests below.
        val expectedByIdEntry = mapOf(
            "fullname" to AutofillView.Identity.PersonNameFull(data = autofillViewData),
            "nameprefix" to AutofillView.Identity.PersonNamePrefix(data = autofillViewData),
            "givenname" to AutofillView.Identity.PersonNameGiven(data = autofillViewData),
            "middlename" to AutofillView.Identity.PersonNameMiddle(data = autofillViewData),
            "surname" to AutofillView.Identity.PersonNameFamily(data = autofillViewData),
            "mailingaddress" to AutofillView.Identity.PostalAddressFull(data = autofillViewData),
            "addressline" to AutofillView.Identity.AddressStreet(data = autofillViewData),
            "town" to AutofillView.Identity.AddressLocality(data = autofillViewData),
            "province" to AutofillView.Identity.AddressRegion(data = autofillViewData),
            "country" to AutofillView.Identity.AddressCountry(data = autofillViewData),
            "postalcode" to AutofillView.Identity.PostalCode(data = autofillViewData),
            "mobile" to AutofillView.Identity.PhoneFull(data = autofillViewData),
            "company" to AutofillView.Identity.Company(data = autofillViewData),
            "socialsecurity" to AutofillView.Identity.Ssn(data = autofillViewData),
            "passport" to AutofillView.Identity.PassportNumber(data = autofillViewData),
            "licensenumber" to AutofillView.Identity.LicenseNumber(data = autofillViewData),
        )

        expectedByIdEntry.forEach { (idEntry, expected) ->
            setupUnsupportedInputFieldViewNode()
            every { viewNode.idEntry } returns idEntry

            val actual = viewNode.toAutofillView(
                parentWebsite = null,
                isIdentityAutofillEnabled = true,
            )

            assertEquals(expected, actual, "idEntry \"$idEntry\" mapped to the wrong view")
        }
    }

    //region Identity: flag-off gate (isIdentityAutofillEnabled = false)

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Unused when autofillHints contain an identity hint and IdentityAutofill is disabled`() {
        setupUnsupportedInputFieldViewNode()
        every {
            viewNode.autofillHints
        } returns arrayOf(HintConstants.AUTOFILL_HINT_PERSON_NAME_GIVEN)

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = false,
        )

        assertEquals(AutofillView.Unused(data = autofillViewData), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Unused when hint is First name and IdentityAutofill is disabled`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.hint } returns "First name"

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = false,
        )

        assertEquals(AutofillView.Unused(data = autofillViewData), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Unused when hint is Mobile number and IdentityAutofill is disabled`() {
        // "Mobile number" only matches the phone identity heuristic (SUPPORTED_RAW_PHONE_HINTS),
        // not isUsernameField's raw hint list, so with the flag off this must fall through to
        // Unused -- not become the focused view via Identity.PhoneFull -- so
        // updateForMissingUsernameFields can still promote it, per the regression this gate
        // guards against.
        setupUnsupportedInputFieldViewNode()
        every { viewNode.hint } returns "Mobile number"

        val actual = viewNode.toAutofillView(
            parentWebsite = null,
            isIdentityAutofillEnabled = false,
        )

        assertEquals(AutofillView.Unused(data = autofillViewData), actual)
    }

    //endregion Identity: flag-off gate (isIdentityAutofillEnabled = false)

    //region Identity: heuristic idEntry fallback + all-null guard (isXxxField)

    @Test
    fun `isPersonNameFullField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "fullname"

        assertTrue(viewNode.isPersonNameFullField)
    }

    @Test
    fun `isPersonNameFullField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPersonNameFullField)
    }

    @Test
    fun `isPersonNamePrefixField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "honorificPrefix"

        assertTrue(viewNode.isPersonNamePrefixField)
    }

    @Test
    fun `isPersonNamePrefixField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPersonNamePrefixField)
    }

    @Test
    fun `isPersonNameGivenField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "first_name"

        assertTrue(viewNode.isPersonNameGivenField)
    }

    @Test
    fun `isPersonNameGivenField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPersonNameGivenField)
    }

    @Test
    fun `isPersonNameMiddleField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "middle_name"

        assertTrue(viewNode.isPersonNameMiddleField)
    }

    @Test
    fun `isPersonNameMiddleField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPersonNameMiddleField)
    }

    @Test
    fun `isPersonNameFamilyField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "last_name"

        assertTrue(viewNode.isPersonNameFamilyField)
    }

    @Test
    fun `isPersonNameFamilyField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPersonNameFamilyField)
    }

    @Test
    fun `isPostalAddressFullField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "shipping_address"

        assertTrue(viewNode.isPostalAddressFullField)
    }

    @Test
    fun `isPostalAddressFullField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPostalAddressFullField)
    }

    @Test
    fun `isAddressStreetField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "street_address"

        assertTrue(viewNode.isAddressStreetField)
    }

    @Test
    fun `isAddressStreetField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isAddressStreetField)
    }

    @Test
    fun `isAddressExtendedField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "address-ext"

        assertTrue(viewNode.isAddressExtendedField)
    }

    @Test
    fun `isAddressExtendedField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isAddressExtendedField)
    }

    @Test
    fun `isAddressLocalityField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "city"

        assertTrue(viewNode.isAddressLocalityField)
    }

    @Test
    fun `isAddressLocalityField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isAddressLocalityField)
    }

    @Test
    fun `isAddressRegionField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "state"

        assertTrue(viewNode.isAddressRegionField)
    }

    @Test
    fun `isAddressRegionField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isAddressRegionField)
    }

    @Test
    fun `isAddressCountryField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "country"

        assertTrue(viewNode.isAddressCountryField)
    }

    @Test
    fun `isAddressCountryField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isAddressCountryField)
    }

    @Test
    fun `isPostalCodeField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "zip_code"

        assertTrue(viewNode.isPostalCodeField)
    }

    @Test
    fun `isPostalCodeField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPostalCodeField)
    }

    @Test
    fun `isPhoneField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "telephone"

        assertTrue(viewNode.isPhoneField)
    }

    @Test
    fun `isPhoneField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPhoneField)
    }

    @Test
    fun `isCompanyField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "organization"

        assertTrue(viewNode.isCompanyField)
    }

    @Test
    fun `isCompanyField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isCompanyField)
    }

    @Test
    fun `isSsnField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "ssn"

        assertTrue(viewNode.isSsnField)
    }

    @Test
    fun `isSsnField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isSsnField)
    }

    @Test
    fun `isPassportNumberField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "passport_number"

        assertTrue(viewNode.isPassportNumberField)
    }

    @Test
    fun `isPassportNumberField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isPassportNumberField)
    }

    @Test
    fun `isLicenseNumberField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "drivers_license"

        assertTrue(viewNode.isLicenseNumberField)
    }

    @Test
    fun `isLicenseNumberField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isLicenseNumberField)
    }

    //endregion Identity: heuristic idEntry fallback + all-null guard (isXxxField)

    //region Identity: isEmailField (dual-classification trigger)

    @Test
    fun `isEmailField returns true when autofillHints match`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.autofillHints } returns arrayOf(View.AUTOFILL_HINT_EMAIL_ADDRESS)

        assertTrue(viewNode.isEmailField)
    }

    @Test
    fun `isEmailField returns true when idEntry is supported`() {
        setupUnsupportedInputFieldViewNode()
        every { viewNode.idEntry } returns "email"

        assertTrue(viewNode.isEmailField)
    }

    @Test
    fun `isEmailField returns false when idEntry, hint, and htmlInfo are all null`() {
        setupUnsupportedInputFieldViewNode()

        assertFalse(viewNode.isEmailField)
    }

    //endregion Identity: isEmailField (dual-classification trigger)

    /**
     * Set up [viewNode] to be an input field but not supported.
     */
    private fun setupUnsupportedInputFieldViewNode() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isPasswordField() } returns false
        every { viewNode.htmlInfo.isUsernameField() } returns false
        every { viewNode.htmlInfo.isInputField } returns true
        every { viewNode.idEntry } returns null
        every { viewNode.autofillHints } returns emptyArray()
        every { viewNode.className } returns null
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns false
        every { viewNode.htmlInfo.hints() } returns emptyList()
        every { viewNode.website } returns null
    }
}

private const val AUTOFILL_OPTION_ONE: String = "AUTOFILL_OPTION_ONE"
private const val AUTOFILL_OPTION_TWO: String = "AUTOFILL_OPTION_TWO"
private val AUTOFILL_OPTIONS_ARRAY: Array<CharSequence> = arrayOf(
    AUTOFILL_OPTION_ONE,
    AUTOFILL_OPTION_TWO,
)
private val AUTOFILL_OPTIONS_LIST: List<String> = listOf(
    AUTOFILL_OPTION_ONE,
    AUTOFILL_OPTION_TWO,
)
private const val AUTOFILL_TYPE: Int = View.AUTOFILL_TYPE_LIST
private val IGNORED_RAW_HINTS: List<String> = listOf(
    "search",
    "find",
    "recipient",
    "edit",
)
private val SUPPORTED_RAW_PASSWORD_HINTS: List<String> = listOf(
    "password",
    "pswd",
)
private val SUPPORTED_RAW_USERNAME_HINTS: List<String> = listOf(
    "email",
    "phone",
    "username",
)
private val SUPPORTED_RAW_CARD_EXP_MONTH_HINTS: List<String> = listOf(
    "exp_month",
    "expiration_month",
    "cc_exp_month",
    "card_exp_month",
)
private val SUPPORTED_RAW_CARD_EXP_YEAR_HINTS: List<String> = listOf(
    "exp_year",
    "expiration_year",
    "cc_exp_year",
    "card_exp_year",
)
private val SUPPORTED_RAW_CARD_NUMBER_HINTS: List<String> = listOf(
    "cc_number",
    "card_number",
    "credit_card_number",
)
private val SUPPORTED_RAW_CARD_SECURITY_CODE_HINTS: List<String> = listOf(
    "cc_security_code",
    "card_security_code",
    "credit_card_security_code",
    "cc_verification_code",
    "card_verification_code",
    "credit_card_verification_code",
    "cvv",
    "cvc",
    "cvv2",
    "cvc2",
)
private val SUPPORTED_RAW_CARD_EXP_DATE_HINTS: List<String> = listOf(
    "exp_date",
    "expiration_date",
    "expiry_date",
    "cc_exp_date",
    "card_exp_date",
)
private val SUPPORTED_RAW_CARDHOLDER_NAME_HINTS: List<String> = listOf(
    "cc_name",
    "cc_cardholder",
    "card_name",
    "card_cardholder",
    "credit_card_name",
    "credit_card_cardholder",
    "name_on_card",
)
private const val MONTH_VALUE: String = "MONTH_VALUE"
private const val YEAR_VALUE: String = "YEAR_VALUE"
private const val TEXT_VALUE: String = "TEXT_VALUE"
