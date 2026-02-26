package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.lifecycle.SavedStateHandle
import com.bitwarden.data.manager.model.FlightRecorderDuration
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 *  View model for the flight recorder configuration screen.
 */
@HiltViewModel
class FlightRecorderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<FlightRecorderState, FlightRecorderEvent, FlightRecorderAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: FlightRecorderState(
            selectedDuration = FlightRecorderDuration.TWENTY_FOUR_HOURS,
        ),
) {
    override fun handleAction(action: FlightRecorderAction) {
        when (action) {
            FlightRecorderAction.BackClick -> handleBackClick()
            is FlightRecorderAction.DurationSelect -> handleOnDurationSelect(action)
            FlightRecorderAction.HelpCenterClick -> handleHelpCenterClick()
            FlightRecorderAction.SaveClick -> handleSaveClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(FlightRecorderEvent.NavigateBack)
    }

    private fun handleOnDurationSelect(action: FlightRecorderAction.DurationSelect) {
        mutableStateFlow.update { it.copy(selectedDuration = action.duration) }
    }

    private fun handleHelpCenterClick() {
        sendEvent(FlightRecorderEvent.NavigateToHelpCenter)
    }

    private fun handleSaveClick() {
        settingsRepository.startFlightRecorder(duration = state.selectedDuration)
        sendEvent(FlightRecorderEvent.NavigateBack)
    }
}

/**
 * Models the UI state for the flight recorder screen.
 */
data class FlightRecorderState(
    val selectedDuration: FlightRecorderDuration,
)

/**
 * Models events for the flight recorder screen.
 */
sealed class FlightRecorderEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : FlightRecorderEvent()

    /**
     * Launches the the help center link.
     */
    data object NavigateToHelpCenter : FlightRecorderEvent()
}

/**
 * Models actions for the flight recorder screen.
 */
sealed class FlightRecorderAction {
    /**
     * Indicates that the user clicked the close button.
     */
    data object BackClick : FlightRecorderAction()

    /**
     * Indicates that the user clicked the help center link.
     */
    data object HelpCenterClick : FlightRecorderAction()

    /**
     * Indicates that the user clicked the save button.
     */
    data object SaveClick : FlightRecorderAction()

    /**
     * Indicates that the user selected a duration.
     */
    data class DurationSelect(
        val duration: FlightRecorderDuration,
    ) : FlightRecorderAction()
}
