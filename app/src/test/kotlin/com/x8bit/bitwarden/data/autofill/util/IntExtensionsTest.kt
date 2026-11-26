package com.x8bit.bitwarden.data.autofill.util

import android.text.InputType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntExtensionsTest {
    @Test
    fun `isPasswordInputType returns false when is multiline`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when not multiline and not password`() {
        // Setup
        val int = InputType.TYPE_CLASS_PHONE

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_WEB_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isPasswordInputType returns true when password variation has other flags set`() {
        // Setup: TYPE_CLASS_TEXT | TYPE_TEXT_FLAG_NO_SUGGESTIONS | TYPE_TEXT_VARIATION_PASSWORD
        val int = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_WEB_EDIT_TEXT`() {
        // Setup: this variation shares the password bit but is not a password
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_FILTER`() {
        // Setup: FILTER (0xb0) contains the password bit (0x80), so a bit test used to match it
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_PHONETIC`() {
        // Setup: PHONETIC (0xc0) contains the password bit (0x80), so a bit test used to match it
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PHONETIC

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`() {
        // Setup: WEB_EMAIL_ADDRESS (0xd0) contains the password bit (0x80), so a bit test used to
        // match it. It is a username input type, never a password.
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when plain TYPE_CLASS_TEXT`() {
        // Setup: the text class on its own has no variation, so it must not be a password
        val int = InputType.TYPE_CLASS_TEXT

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when multiline TYPE_TEXT_VARIATION_WEB_EDIT_TEXT`() {
        // Setup: a WebView textarea
        val int = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false for the password variation without a text class`() {
        // Setup: some apps report only the variation bits on a user ID field (Truist reports
        // `inputType=128`). Without TYPE_CLASS_TEXT it is not a password.
        val int = InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false for NO_SUGGESTIONS and VISIBLE_PASSWORD without a class`() {
        // Setup: the common snippet for turning off suggestions on a username field. Mazda
        // Financial Services reports this as `inputType=524432`.
        val int = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertEquals(524432, int)
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns true for the text class with NO_SUGGESTIONS and PASSWORD`() {
        // Setup: the password field in Truist and Mazda Financial Services (`inputType=524417`)
        val int = 524417

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isUsernameInputType returns false when not TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`() {
        // Setup
        val int = InputType.TYPE_CLASS_PHONE

        // Test
        val actual = int.isUsernameInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isUsernameInputType returns true when TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`() {
        // Setup
        val int = InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

        // Test
        val actual = int.isUsernameInputType

        // Verify
        assertTrue(actual)
    }
}
