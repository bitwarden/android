package com.bitwarden.ui.platform.feature.cardscanner.util

private val PAN_REGEX = Regex("""\b(?:\d[ -]*?){13,19}\b""")

private val EXPIRY_REGEX = Regex("""\b(0[1-9]|1[0-2])\s?[/\-]\s?(\d{2}|\d{4})\b""")

private val CVV3_REGEX = Regex("""\b\d{3}\b""")
private val CVV4_REGEX = Regex("""\b\d{4}\b""")

private val NAME_REGEX = Regex("""^[A-Z][A-Z .'-]+$""")

/**
 * Default [CardDataParser] implementation that uses regex patterns
 * and Luhn validation to extract card details from OCR text.
 */
class CardDataParserImpl : CardDataParser {

    @Suppress("MagicNumber")
    override fun parseCardData(text: String): CardScanData {
        val panMatch = PAN_REGEX.find(text)
        val number = panMatch
            ?.value
            ?.filter { it.isDigit() }
            ?.takeIf { it.isValidLuhn() }

        val expiryMatch = EXPIRY_REGEX.find(text)
        val expirationMonth = expiryMatch
            ?.groupValues
            ?.getOrNull(1)
        val expirationYear = expiryMatch
            ?.groupValues
            ?.getOrNull(2)
            ?.let { if (it.length == 2) "20$it" else it }

        // Use brand-aware CVV length: Amex uses 4 digits, all others use 3.
        val isAmex = number?.let { it.startsWith("34") || it.startsWith("37") } == true
        val cvvRegex = if (isAmex) CVV4_REGEX else CVV3_REGEX

        // Filter out digits adjacent to other digits (likely phone numbers)
        // or that overlap with already-matched PAN/expiry ranges.
        val panRange = panMatch?.range
        val expiryRange = expiryMatch?.range
        val securityCode = cvvRegex
            .findAll(text)
            .lastOrNull { match ->
                panRange?.contains(match.range.first) != true &&
                    expiryRange?.contains(match.range.first) != true &&
                    text.getOrNull(match.range.first - 1)?.isDigit() != true &&
                    text.getOrNull(match.range.last + 1)?.isDigit() != true
            }
            ?.value

        return CardScanData(
            number = number,
            expirationMonth = expirationMonth,
            expirationYear = expirationYear,
            cardholderName = extractCardholderName(text),
            securityCode = securityCode,
        )
    }
}

@Suppress("MagicNumber")
private fun extractCardholderName(text: String): String? =
    text.lines()
        .map { it.trim() }
        .filter { it.length > 3 }
        .firstOrNull { NAME_REGEX.matches(it) }
