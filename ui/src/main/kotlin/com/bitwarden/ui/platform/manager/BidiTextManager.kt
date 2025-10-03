package com.bitwarden.ui.platform.manager

import com.bitwarden.annotation.OmitFromCoverage

/**
 * Manages bidirectional text handling, ensuring proper display of text containing both
 * left-to-right (LTR) and right-to-left (RTL) characters. This is crucial for internationalization
 * and supporting languages like Arabic, Hebrew, etc.
 *
 * This interface provides methods to wrap text with Unicode directionality control characters
 * (`\u202A` for LTR and `\u202B` for RTL) to enforce a consistent base direction, preventing
 * mixed-direction text from being garbled.
 */
interface BidiTextManager {
    /**
     * Wraps a [String] with unicode directionality characters to ensure it's displayed correctly
     * regardless of the System's default layout direction.
     *
     * This is useful for displaying text that might contain mixed right-to-left (RTL) and
     * left-to-right (LTR) content, preventing it from being incorrectly ordered or garbled.
     *
     * For example, an email address like "user@example.com" could be displayed incorrectly
     * in an RTL context. This function wraps it to enforce LTR rendering.
     *
     * @param text The string to be wrapped.
     * @return The wrapped string, ready for display in a BiDi-sensitive context.
     */
    fun unicodeWrap(text: String): String

    @Suppress("UndocumentedPublicClass")
    @OmitFromCoverage
    companion object {
        /**
         * Creates a new [BidiTextManager] instance.
         */
        fun create(): BidiTextManager = BidiTextManagerImpl()
    }
}
