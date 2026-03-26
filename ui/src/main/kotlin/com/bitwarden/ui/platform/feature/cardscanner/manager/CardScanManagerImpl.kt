package com.bitwarden.ui.platform.feature.cardscanner.manager

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Primary implementation of [CardScanManager].
 */
class CardScanManagerImpl : CardScanManager {

    private val mutableCardScanResultFlow =
        bufferedMutableSharedFlow<CardScanResult>()

    override val cardScanResultFlow: Flow<CardScanResult>
        get() = mutableCardScanResultFlow.asSharedFlow()

    override fun emitCardScanResult(cardScanResult: CardScanResult) {
        mutableCardScanResultFlow.tryEmit(cardScanResult)
    }
}
