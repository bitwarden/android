package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import com.bitwarden.ui.platform.feature.cardscanner.manager.CardScanManager
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanData
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * The duration the scanner waits for a successful scan before surfacing a hint to the user
 * suggesting they hold the device steady so all card details are visible.
 */
private const val SCAN_HINT_TIMEOUT_MS = 5_000L

/**
 * Handles [CardScanAction] and launches [CardScanEvent] for the [CardScanScreen].
 */
@HiltViewModel
class CardScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardScanManager: CardScanManager,
) : BaseViewModel<CardScanState, CardScanEvent, CardScanAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: CardScanState(hasHandledScan = false, showHint = false),
) {

    private var hintTimeoutJob: Job? = null

    init {
        startHintTimeout()
    }

    override fun onCleared() {
        hintTimeoutJob?.cancel()
        super.onCleared()
    }

    override fun handleAction(action: CardScanAction) {
        when (action) {
            is CardScanAction.CloseClick -> handleCloseClick()
            is CardScanAction.CameraSetupErrorReceive -> handleCameraErrorReceive()
            is CardScanAction.CardScanReceive -> handleCardScanReceive(action)
            is CardScanAction.Internal.HintTimeoutElapsed -> handleHintTimeoutElapsed()
        }
    }

    private fun handleCloseClick() {
        hintTimeoutJob?.cancel()
        sendEvent(CardScanEvent.NavigateBack)
    }

    private fun handleCameraErrorReceive() {
        hintTimeoutJob?.cancel()
        cardScanManager.emitCardScanResult(CardScanResult.ScanError())
        sendEvent(CardScanEvent.NavigateBack)
    }

    private fun handleCardScanReceive(action: CardScanAction.CardScanReceive) {
        if (state.hasHandledScan) return
        hintTimeoutJob?.cancel()
        mutableStateFlow.update {
            it.copy(hasHandledScan = true, showHint = false)
        }
        cardScanManager.emitCardScanResult(
            CardScanResult.Success(cardScanData = action.cardScanData),
        )
        sendEvent(CardScanEvent.NavigateBack)
    }

    private fun handleHintTimeoutElapsed() {
        if (state.hasHandledScan) return
        mutableStateFlow.update { it.copy(showHint = true) }
    }

    private fun startHintTimeout() {
        hintTimeoutJob?.cancel()
        hintTimeoutJob = viewModelScope.launch {
            delay(SCAN_HINT_TIMEOUT_MS)
            sendAction(CardScanAction.Internal.HintTimeoutElapsed)
        }
    }
}

/**
 * Models events for the [CardScanScreen].
 */
sealed class CardScanEvent {

    /**
     * Navigate back. Added [DeferredBackgroundEvent] as scan might fire before
     * events are consumed.
     */
    data object NavigateBack : CardScanEvent(), DeferredBackgroundEvent
}

/**
 * Models actions for the [CardScanScreen].
 */
sealed class CardScanAction {

    /**
     * User clicked close.
     */
    data object CloseClick : CardScanAction()

    /**
     * A card has been scanned with the detected fields.
     */
    data class CardScanReceive(
        val cardScanData: CardScanData,
    ) : CardScanAction()

    /**
     * The camera is unable to be set up.
     */
    data object CameraSetupErrorReceive : CardScanAction()

    /**
     * Models actions that the [CardScanViewModel] itself might send.
     */
    sealed class Internal : CardScanAction() {

        /**
         * The hint timeout has elapsed without a successful scan.
         */
        data object HintTimeoutElapsed : Internal()
    }
}

/**
 * Represents the state of the card scan screen.
 */
@Parcelize
data class CardScanState(
    val hasHandledScan: Boolean,
    val showHint: Boolean,
) : Parcelable
