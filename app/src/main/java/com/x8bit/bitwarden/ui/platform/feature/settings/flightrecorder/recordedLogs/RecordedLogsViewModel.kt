package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.data.platform.datasource.disk.model.FlightRecorderDataSet
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util.toViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the flight recorder recorded logs screen.
 */
@HiltViewModel
class RecordedLogsViewModel @Inject constructor(
    private val clock: Clock,
    private val settingsRepository: SettingsRepository,
    fileManager: FileManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<RecordedLogsState, RecordedLogsEvent, RecordedLogsAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: RecordedLogsState(
            viewState = RecordedLogsState.ViewState.Loading,
            logsFolder = fileManager.logsDirectory,
        ),
) {
    init {
        settingsRepository
            .flightRecorderDataFlow
            .map { RecordedLogsAction.Internal.OnReceiveFlightRecorderData(it) }
            .onEach(::sendAction)
            .launchIn(scope = viewModelScope)
    }

    override fun handleAction(action: RecordedLogsAction) {
        when (action) {
            RecordedLogsAction.BackClick -> handleBackClick()
            RecordedLogsAction.DeleteAllClick -> handleDeleteAllClick()
            is RecordedLogsAction.DeleteClick -> handleDeleteClick(action)
            is RecordedLogsAction.ShareClick -> handleShareClick(action)
            RecordedLogsAction.ShareAllClick -> handleShareAllClick()
            is RecordedLogsAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(RecordedLogsEvent.NavigateBack)
    }

    private fun handleDeleteAllClick() {
        settingsRepository.deleteAllLogs()
    }

    private fun handleDeleteClick(action: RecordedLogsAction.DeleteClick) {
        settingsRepository
            .flightRecorderData
            .data
            .find { it.id == action.item.id }
            ?.let { settingsRepository.deleteLog(data = it) }
    }

    private fun handleShareAllClick() {
        // TODO: PM-19622 Add logic for sharing the logs
    }

    private fun handleShareClick(action: RecordedLogsAction.ShareClick) {
        // TODO: PM-19622 Add logic for sharing the logs
    }

    private fun handleInternalAction(action: RecordedLogsAction.Internal) {
        when (action) {
            is RecordedLogsAction.Internal.OnReceiveFlightRecorderData -> {
                handleOnReceiveFlightRecorderData(action)
            }
        }
    }

    private fun handleOnReceiveFlightRecorderData(
        action: RecordedLogsAction.Internal.OnReceiveFlightRecorderData,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = action.flightRecorderData.toViewState(
                    clock = clock,
                    logsFolder = state.logsFolder,
                ),
            )
        }
    }
}

/**
 * Models the UI state for the recorded logs screen.
 */
data class RecordedLogsState(
    val viewState: ViewState,
    val logsFolder: String,
) {
    /**
     * View states for the [RecordedLogsViewModel].
     */
    sealed class ViewState {
        /**
         * Indicates if the overflow items should be enabled.
         */
        abstract val isOverflowEnabled: Boolean

        /**
         * Represents the loading state for the [RecordedLogsViewModel].
         */
        data object Loading : ViewState() {
            override val isOverflowEnabled: Boolean get() = false
        }

        /**
         * Represents the empty state for the [RecordedLogsViewModel].
         */
        data object Empty : ViewState() {
            override val isOverflowEnabled: Boolean get() = false
        }

        /**
         * Represents the content state for the [RecordedLogsViewModel].
         */
        data class Content(
            val items: ImmutableList<DisplayItem>,
        ) : ViewState() {
            override val isOverflowEnabled: Boolean get() = true
        }
    }

    /**
     * Wrapper class for all displayable data in a row.
     */
    data class DisplayItem(
        val id: String,
        val title: Text,
        val subtextStart: Text,
        val subtextEnd: Text?,
        val isDeletedEnabled: Boolean,
    )
}

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

    /**
     * Indicates that the user clicked the delete all button.
     */
    data object DeleteAllClick : RecordedLogsAction()

    /**
     * Indicates that the user clicked the delete button for a specific item.
     */
    data class DeleteClick(
        val item: RecordedLogsState.DisplayItem,
    ) : RecordedLogsAction()

    /**
     * Indicates that the user clicked the share all button.
     */
    data object ShareAllClick : RecordedLogsAction()

    /**
     * Indicates that the user clicked the share button for a specific item.
     */
    data class ShareClick(
        val item: RecordedLogsState.DisplayItem,
    ) : RecordedLogsAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : RecordedLogsAction() {
        /**
         * Indicates that the log data has changed.
         */
        data class OnReceiveFlightRecorderData(
            val flightRecorderData: FlightRecorderDataSet,
        ) : Internal()
    }
}
