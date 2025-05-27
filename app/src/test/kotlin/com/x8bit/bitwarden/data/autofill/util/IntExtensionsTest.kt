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
