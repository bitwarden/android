package com.x8bit.bitwarden.ui.vault.util

import com.bitwarden.ui.platform.feature.cardscanner.util.sanitizeCardNumber
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand

/**
 * Detects the card brand based on the card number prefix.
 *
 * @return The detected [VaultCardBrand], or [VaultCardBrand.OTHER] if no match is found.
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber")
fun String.detectCardBrand(): VaultCardBrand {
    val digits = sanitizeCardNumber()
    if (digits.isEmpty()) return VaultCardBrand.OTHER

    return when {
        // Amex: starts with 34 or 37
        digits.startsWith("34") || digits.startsWith("37") -> VaultCardBrand.AMEX

        // Visa: starts with 4
        digits.startsWith("4") -> VaultCardBrand.VISA

        // Mastercard: 51-55 or 2221-2720
        digits.isMastercardPrefix() -> VaultCardBrand.MASTERCARD

        // Discover: 6011, 65, 644-649
        digits.isDiscoverPrefix() -> VaultCardBrand.DISCOVER

        // Diners Club: 300-305, 36, 38
        digits.isDinersClubPrefix() -> VaultCardBrand.DINERS_CLUB

        // JCB: 3528-3589
        digits.isJcbPrefix() -> VaultCardBrand.JCB

        // Maestro: 5018, 5020, 5038, 6304
        digits.isMaestroPrefix() -> VaultCardBrand.MAESTRO

        // UnionPay: starts with 62
        digits.startsWith("62") -> VaultCardBrand.UNIONPAY

        // RuPay: 60, 65, 81, 82
        digits.isRuPayPrefix() -> VaultCardBrand.RUPAY

        else -> VaultCardBrand.OTHER
    }
}

@Suppress("MagicNumber")
private fun String.isMastercardPrefix(): Boolean {
    if (length < 2) return false
    val twoDigit = substring(0, 2).toIntOrNull() ?: return false
    if (twoDigit in 51..55) return true
    if (length < 4) return false
    val fourDigit = substring(0, 4).toIntOrNull() ?: return false
    return fourDigit in 2221..2720
}

@Suppress("MagicNumber")
private fun String.isDiscoverPrefix(): Boolean {
    if (startsWith("6011") || startsWith("65")) return true
    if (length < 3) return false
    val threeDigit = substring(0, 3).toIntOrNull() ?: return false
    return threeDigit in 644..649
}

@Suppress("MagicNumber")
private fun String.isDinersClubPrefix(): Boolean {
    if (startsWith("36") || startsWith("38")) return true
    if (length < 3) return false
    val threeDigit = substring(0, 3).toIntOrNull() ?: return false
    return threeDigit in 300..305
}

@Suppress("MagicNumber")
private fun String.isJcbPrefix(): Boolean {
    if (length < 4) return false
    val fourDigit = substring(0, 4).toIntOrNull() ?: return false
    return fourDigit in 3528..3589
}

private fun String.isMaestroPrefix(): Boolean =
    startsWith("5018") ||
        startsWith("5020") ||
        startsWith("5038") ||
        startsWith("6304")

// Note: "60" and "65" overlap with Discover prefixes ("6011", "65") but are
// unreachable here because Discover is checked first in detectCardBrand().
// They are kept for documentation of the full RuPay prefix specification.
private fun String.isRuPayPrefix(): Boolean =
    startsWith("60") ||
        startsWith("65") ||
        startsWith("81") ||
        startsWith("82")
