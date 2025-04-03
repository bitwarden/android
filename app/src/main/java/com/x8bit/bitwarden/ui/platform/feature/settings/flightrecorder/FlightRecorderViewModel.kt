package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 *  View model for the flight recorder configuration screen.
 */
@HiltViewModel
class FlightRecorderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<FlightRecorderState, FlightRecorderEvent, FlightRecorderAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: FlightRecorderState,
) {
    override fun handleAction(action: FlightRecorderAction) {
        when (action) {
            FlightRecorderAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(FlightRecorderEvent.NavigateBack)
    }
}

/**
 * Models the UI state for the flight recorder screen.
 */
data object FlightRecorderState

/**
 * Models events for the flight recorder screen.
 */
sealed class FlightRecorderEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : FlightRecorderEvent()
}

/**
 * Models actions for the flight recorder screen.
 */
sealed class FlightRecorderAction {
    /**
     * Indicates that the user clicked the close button.
     */
    data object BackClick : FlightRecorderAction()
}
