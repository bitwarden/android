package com.x8bit.bitwarden.data.autofill.util

import android.app.assist.AssistStructure
import android.view.View
import android.view.ViewStructure.HtmlInfo
import android.view.autofill.AutofillId
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

class ViewNodeExtensionsTest {
    private val expectedAutofillId: AutofillId = mockk()
    private val expectedIsFocused = true
    private val autofillViewData = AutofillView.Data(
        autofillId = expectedAutofillId,
        isFocused = expectedIsFocused,
    )

    private val viewNode: AssistStructure.ViewNode = mockk {
        every { this@mockk.autofillId } returns expectedAutofillId
        every { this@mockk.childCount } returns 0
        every { this@mockk.inputType } returns 1
        every { this@mockk.isFocused } returns expectedIsFocused
    }

    @BeforeEach
    fun setup() {
        mockkStatic(HtmlInfo::isInputField)
        mockkStatic(HtmlInfo::isPasswordField)
        mockkStatic(Int::isPasswordInputType)
        mockkStatic(Int::isUsernameInputType)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(HtmlInfo::isInputField)
        unmockkStatic(HtmlInfo::isPasswordField)
        unmockkStatic(Int::isPasswordInputType)
        unmockkStatic(Int::isUsernameInputType)
    }

    @Test
    fun `toAutofillView should return AutofillView Card ExpirationMonth when hint matches`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH
        val expected = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
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
            data = autofillViewData,
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
            data = autofillViewData,
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
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillView should return AutofillView Login Password when isPasswordField`() {
        // Setup
        val autofillHint = View.AUTOFILL_HINT_PASSWORD
        val expected = AutofillView.Login.Password(
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHint)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return AutofillView Login Username when is android text field and is isUsernameField`() {
        // Setup
        val expected = AutofillView.Login.Username(
            data = autofillViewData,
        )
        setupUnsupportedInputFieldViewNode()
        every { viewNode.className } returns ANDROID_EDIT_TEXT_CLASS_NAME
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns true

        // Test
        val actual = viewNode.toAutofillView()

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
        val actual = viewNode.toAutofillView()

        // Verify
        assertNull(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillView should return null when hint is not supported, is an inputField, and isn't a username or password`() {
        // Setup
        setupUnsupportedInputFieldViewNode()

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
            data = autofillViewData,
        )
        every { viewNode.autofillHints } returns arrayOf(autofillHintOne, autofillHintTwo)

        // Test
        val actual = viewNode.toAutofillView()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `isPasswordField returns true when supportedHint is AUTOFILL_HINT_PASSWORD`() {
        // Setup
        val supportedHint = View.AUTOFILL_HINT_PASSWORD

        // Test
        val actual = viewNode.isPasswordField(
            supportedHint = supportedHint,
        )

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isPasswordField returns true when supportedHint is null and hint is supported`() {
        SUPPORTED_RAW_PASSWORD_HINTS
            .forEach { hint ->
                // Setup
                every { viewNode.hint } returns hint

                // Test
                val actual = viewNode.isPasswordField(
                    supportedHint = null,
                )

                // Verify
                assertTrue(actual)
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordField returns true when hints aren't supported, isPasswordInputType, isValidField, and isn't username`() {
        // Setup
        setupUnsupportedInputFieldViewNode()
        every { any<Int>().isPasswordInputType } returns true

        // Test
        val actual = viewNode.isPasswordField(
            supportedHint = null,
        )

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
        val actual = viewNode.isPasswordField(
            supportedHint = null,
        )

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
            val actual = viewNode.isPasswordField(
                supportedHint = null,
            )

            // Verify
            assertFalse(actual)
        }

        // Setup testing the idEntry
        every { viewNode.hint } returns null
        IGNORED_RAW_HINTS.forEach { hint ->
            // Setup
            every { viewNode.idEntry } returns hint

            // Test
            val actual = viewNode.isPasswordField(
                supportedHint = null,
            )

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
        val actual = viewNode.isPasswordField(
            supportedHint = null,
        )

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isUsernameField returns true whe supportedHint is AUTOFILL_HINT_USERNAME`() {
        // Setup
        val supportedHint = View.AUTOFILL_HINT_USERNAME

        // Test
        val actual = viewNode.isUsernameField(
            supportedHint = supportedHint,
        )

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isUsernameField returns true when supportedHint is AUTOFILL_HINT_EMAIL_ADDRESS`() {
        // Setup
        val supportedHint = View.AUTOFILL_HINT_EMAIL_ADDRESS

        // Test
        val actual = viewNode.isUsernameField(
            supportedHint = supportedHint,
        )

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
            val actual = viewNode.isUsernameField(
                supportedHint = null,
            )

            // Verify
            assertTrue(actual)
        }

        // Setup testing the idEntries
        every { viewNode.hint } returns null
        SUPPORTED_RAW_USERNAME_HINTS.forEach { hint ->
            // Setup
            every { viewNode.idEntry } returns hint

            // Test
            val actual = viewNode.isUsernameField(
                supportedHint = null,
            )

            // Verify
            assertTrue(actual)
        }
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

    /**
     * Set up [viewNode] to be an input field but not supported.
     */
    private fun setupUnsupportedInputFieldViewNode() {
        every { viewNode.hint } returns null
        every { viewNode.htmlInfo.isPasswordField() } returns false
        every { viewNode.htmlInfo.isInputField } returns true
        every { viewNode.idEntry } returns null
        every { viewNode.autofillHints } returns emptyArray()
        every { viewNode.className } returns null
        every { any<Int>().isPasswordInputType } returns false
        every { any<Int>().isUsernameInputType } returns false
    }
}

private const val ANDROID_EDIT_TEXT_CLASS_NAME: String = "android.widget.EditText"
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
