package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the flight recorder recorded logs screen.
 */
@HiltViewModel
class RecordedLogsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<RecordedLogsState, RecordedLogsEvent, RecordedLogsAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: RecordedLogsState,
) {
    override fun handleAction(action: RecordedLogsAction) {
        when (action) {
            RecordedLogsAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(RecordedLogsEvent.NavigateBack)
    }
}

/**
 * Models the UI state for the recorded logs screen.
 */
data object RecordedLogsState

/**
 * Models events for the recorded logs screen.
 */
sealed class RecordedLogsEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : RecordedLogsEvent()
}

/**
 * Models actions for the recorded logs screen.
 */
sealed class RecordedLogsAction {
    /**
     * Indicates that the user clicked the close button.
     */
    data object BackClick : RecordedLogsAction()
}
