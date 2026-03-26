package com.x8bit.bitwarden.ui.vault.feature.cardscanner

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanData
import com.bitwarden.ui.platform.feature.cardscanner.manager.CardScanManager
import com.bitwarden.ui.platform.feature.cardscanner.util.CardScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Handles [CardScanAction] and launches [CardScanEvent] for the [CardScanScreen].
 */
@HiltViewModel
class CardScanViewModel @Inject constructor(
    private val cardScanManager: CardScanManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CardScanState, CardScanEvent, CardScanAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: CardScanState(hasHandledScan = false),
) {
    override fun handleAction(action: CardScanAction) {
        when (action) {
            is CardScanAction.CloseClick -> handleCloseClick()
            is CardScanAction.CameraSetupErrorReceive -> handleCameraErrorReceive()
            is CardScanAction.CardScanReceive -> handleCardScanReceive(action)
        }
    }

    private fun handleCloseClick() {
        sendEvent(CardScanEvent.NavigateBack)
    }

    private fun handleCameraErrorReceive() {
        cardScanManager.emitCardScanResult(CardScanResult.ScanError())
        sendEvent(CardScanEvent.NavigateBack)
    }

    private fun handleCardScanReceive(action: CardScanAction.CardScanReceive) {
        if (state.hasHandledScan) return
        mutableStateFlow.update { it.copy(hasHandledScan = true) }
        cardScanManager.emitCardScanResult(
            CardScanResult.Success(cardScanData = action.cardScanData),
        )
        sendEvent(CardScanEvent.NavigateBack)
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
}

/**
 * Represents the state of the card scan screen.
 */
@Parcelize
data class CardScanState(
    val hasHandledScan: Boolean,
) : Parcelable
