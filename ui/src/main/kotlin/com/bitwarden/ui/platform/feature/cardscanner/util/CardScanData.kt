package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Data class representing the results of a credit card scan.
 *
 * @property number The detected card number.
 * @property expirationMonth The detected expiration month (01-12).
 * @property expirationYear The detected expiration year.
 * @property securityCode The detected security code.
 */
data class CardScanData(
    val number: String?,
    val expirationMonth: String?,
    val expirationYear: String?,
    val securityCode: String?,
) {
    override fun toString(): String = "CardScanData(number=****," +
        " expirationMonth=$expirationMonth," +
        " expirationYear=$expirationYear," +
        " securityCode=****)"
}
