package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Parses raw OCR text from a credit card scan and extracts structured
 * card data fields.
 */
interface CardDataParser {

    /**
     * Parses the given [text] and returns a [CardScanData] containing
     * any detected card details.
     */
    fun parseCardData(text: String): CardScanData
}
