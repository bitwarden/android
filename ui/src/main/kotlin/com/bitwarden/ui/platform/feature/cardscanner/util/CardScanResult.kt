package com.bitwarden.ui.platform.feature.cardscanner.util

/**
 * Models result of the user scanning a credit card.
 */
sealed class CardScanResult {

    /**
     * Card has been successfully scanned with the detected fields.
     *
     * @property cardScanData The scanned card data.
     */
    data class Success(val cardScanData: CardScanData) : CardScanResult()

    /**
     * There was an error scanning the card.
     */
    data class ScanError(val error: Throwable? = null) : CardScanResult()
}
