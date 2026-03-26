package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Sanitizes a raw card number string by removing all non-digit characters.
 */
fun String.sanitizeCardNumber(): String = filter { it.isDigit() }

/**
 * Validates a card number using the Luhn algorithm.
 *
 * The receiver is first sanitized to remove non-digit characters before
 * validation. Card numbers must be between 13 and 19 digits in length.
 *
 * @return `true` if the card number passes the Luhn check.
 */
@Suppress("MagicNumber")
fun String.isValidLuhn(): Boolean {
    val digits = sanitizeCardNumber()
    if (digits.length !in 13..19) return false

    var sum = 0
    var alternate = false
    for (i in digits.lastIndex downTo 0) {
        var n = digits[i] - '0'
        if (alternate) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        alternate = !alternate
    }
    return sum % 10 == 0
}
