package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Parses raw OCR text from a credit card scan and extracts structured
 * card data fields.
 */
interface CardDataParser {

    /**
     * Parses the given [text] and returns a [ParsedCardFields] containing any detected card
     * fields, or `null` if no card data is found. Individual fields on the returned value may be
     * `null` because OCR may surface only a subset of fields per frame; the downstream pipeline is
     * responsible for assembling a confirmed [CardScanData] from accumulated observations.
     */
    fun parseCardData(text: String): ParsedCardFields?
}
