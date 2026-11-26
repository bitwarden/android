package com.x8bit.bitwarden.data.autofill.util

import android.text.InputType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntExtensionsTest {
    @Test
    fun `isPasswordInputType returns false when is multiline`() {
        // Setup
        val int = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_MULTI_LINE

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
        val int = InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_WEB_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

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
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_PHONETIC`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PHONETIC

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Test
    fun `isPasswordInputType returns false when plain TYPE_CLASS_TEXT`() {
        // Setup
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
    fun `isPasswordInputType returns true for the password variation without a text class`() {
        // Setup: some apps report only the variation bits (for example `inputType=128`)
        val int = 128

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
