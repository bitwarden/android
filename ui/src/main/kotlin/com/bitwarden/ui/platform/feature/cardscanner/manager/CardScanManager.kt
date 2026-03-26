package com.bitwarden.ui.platform.feature.cardscanner.manager

import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Manages the communication of credit card scan results between the card scanner
 * screen and the vault add/edit screen.
 */
interface CardScanManager {

    /**
     * Flow that emits card scan results.
     */
    val cardScanResultFlow: Flow<CardScanResult>

    /**
     * Emits a [CardScanResult] to all active subscribers.
     */
    fun emitCardScanResult(cardScanResult: CardScanResult)
}
