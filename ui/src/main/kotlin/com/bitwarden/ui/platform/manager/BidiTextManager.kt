package com.bitwarden.ui.platform.manager

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

    /**
     * Forces left-to-right (LTR) display direction for the given text by wrapping it with
     * Unicode LTR embedding marks (U+202A...U+202C).
     *
     * Use this for content that should always display left-to-right regardless of system locale,
     * such as URLs, code snippets, numeric codes, or technical identifiers.
     *
     * @param text The text to force as LTR.
     * @return The text wrapped with LTR embedding marks.
     */
    fun forceLtr(text: String): String

    /**
     * Forces right-to-left (RTL) display direction for the given text by wrapping it with
     * Unicode RTL embedding marks (U+202B...U+202C).
     *
     * Use this for content that should always display right-to-left, such as Arabic or Hebrew
     * text in an otherwise LTR context.
     *
     * @param text The text to force as RTL.
     * @return The text wrapped with RTL embedding marks.
     */
    fun forceRtl(text: String): String

    /**
     * Formats a verification code (such as TOTP) by grouping digits into chunks and ensuring
     * left-to-right display direction.
     *
     * Example: "123456" with chunkSize=3 becomes "123 456" displayed as LTR.
     *
     * @param code The verification code to format.
     * @param chunkSize The size of each chunk (default is 3).
     * @return The formatted verification code with LTR directionality.
     */
    fun formatVerificationCode(code: String, chunkSize: Int = 3): String

    /**
     * Formats a phone number to ensure left-to-right display direction.
     *
     * Phone numbers should always display LTR regardless of system locale to maintain
     * international formatting conventions.
     *
     * @param phone The phone number to format.
     * @return The phone number with LTR directionality.
     */
    fun formatPhoneNumber(phone: String): String

    /**
     * Formats a credit/debit card number by ensuring left-to-right display direction.
     *
     * @param number The card number to format.
     * @return The formatted card number with LTR directionality.
     */
    fun formatCardNumber(number: String): String
}
