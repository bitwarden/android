package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * A confirmed credit card scan result delivered to consumers of [CardScanResult.Success].
 *
 * The card scanner pipeline emits this only after [number] has been confirmed by temporal voting
 * and an [expirationMonth] has been observed in the same window, so both are guaranteed non-null
 * at this boundary. [expirationYear] remains optional because expiry text occasionally contains
 * only a month, and [securityCode] remains optional because the CVV may not be visible in the
 * captured frame.
 *
 * @property number The detected card number.
 * @property expirationMonth The detected expiration month (01-12).
 * @property expirationYear The detected expiration year, or `null` if not observed.
 * @property securityCode The detected security code, or `null` if not observed.
 */
data class CardScanData(
    val number: String,
    val expirationMonth: String,
    val expirationYear: String?,
    val securityCode: String?,
) {
    override fun toString(): String = "CardScanData(number=****," +
        " expirationMonth=$expirationMonth," +
        " expirationYear=$expirationYear," +
        " securityCode=****)"
}
