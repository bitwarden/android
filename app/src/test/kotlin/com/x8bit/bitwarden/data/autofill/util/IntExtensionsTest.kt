package com.x8bit.bitwarden.data.autofill.util

import android.text.InputType
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
    fun `isPasswordInputType returns true when not multiline and TYPE_TEXT_VARIATION_WEB_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isPasswordInputType returns true when password variation has additional text flags`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertTrue(actual)
    }

    @Test
    fun `isPasswordInputType returns false for TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`() {
        // Setup
        val int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isPasswordInputType returns false for text variations that share bits with password variations`() {
        // These variations are enum values whose bits overlap TYPE_TEXT_VARIATION_PASSWORD (0x80),
        // so a bit-flag check incorrectly treats them as passwords.
        listOf(
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT,
            InputType.TYPE_TEXT_VARIATION_FILTER,
            InputType.TYPE_TEXT_VARIATION_PHONETIC,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        ).forEach { variation ->
            // Setup
            val int = InputType.TYPE_CLASS_TEXT or variation

            // Test
            val actual = int.isPasswordInputType

            // Verify
            assertFalse(actual, "variation=0x${variation.toString(radix = 16)}")
        }
    }

    @Test
    fun `isPasswordInputType returns false for a non text class with a password-like variation`() {
        // Setup
        val int = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Test
        val actual = int.isPasswordInputType

        // Verify
        assertFalse(actual)
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
