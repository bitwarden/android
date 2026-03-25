package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Data class representing the results of a credit card scan.
 *
 * @property number The detected card number.
 * @property expirationMonth The detected expiration month (01-12).
 * @property expirationYear The detected expiration year.
 * @property cardholderName The detected cardholder name.
 * @property securityCode The detected security code.
 */
data class CardScanData(
    val number: String? = null,
    val expirationMonth: String? = null,
    val expirationYear: String? = null,
    val cardholderName: String? = null,
    val securityCode: String? = null,
)
