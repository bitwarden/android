package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Loose tuple of card fields extracted from a single OCR pass. Any individual field may be `null`
 * because the parser surfaces whatever it can find in the frame; downstream gating in
 * [CardTextAnalyzer] is responsible for assembling a confirmed [CardScanData] only once the
 * required fields have been observed.
 *
 * @property number The detected card number, or `null` if absent or fails Luhn validation.
 * @property expirationMonth The detected expiration month (01-12), or `null` if absent.
 * @property expirationYear The detected expiration year, or `null` if absent.
 * @property securityCode The detected security code, or `null` if absent.
 */
data class ParsedCardFields(
    val number: String?,
    val expirationMonth: String?,
    val expirationYear: String?,
    val securityCode: String?,
) {
    override fun toString(): String = "ParsedCardFields(number=****," +
        " expirationMonth=$expirationMonth," +
        " expirationYear=$expirationYear," +
        " securityCode=****)"
}
