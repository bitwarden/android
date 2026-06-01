package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.manager.model.ZipFileResult
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder.recordedLogs.util.toViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
    private val fileManager: FileManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<RecordedLogsState, RecordedLogsEvent, RecordedLogsAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: RecordedLogsState(
            viewState = RecordedLogsState.ViewState.Loading,
            dialogState = null,
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
            RecordedLogsAction.DismissDialog -> handleDismissDialog()
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

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleDeleteAllClick() {
        settingsRepository.deleteAllLogs()
        sendEvent(RecordedLogsEvent.ShowSnackbar(text = BitwardenString.all_logs_deleted.asText()))
    }

    private fun handleDeleteClick(action: RecordedLogsAction.DeleteClick) {
        settingsRepository
            .flightRecorderData
            .data
            .find { it.id == action.item.id }
            ?.let {
                settingsRepository.deleteLog(data = it)
                sendEvent(
                    RecordedLogsEvent.ShowSnackbar(
                        text = BitwardenString.log_deleted.asText(),
                    ),
                )
            }
    }

    private fun handleShareAllClick() {
        viewModelScope.launch {
            val result = fileManager.zipUriToCache(uri = fileManager.logsDirectory.toUri())
            sendAction(RecordedLogsAction.Internal.OnReceiveShareLogResult(result))
        }
    }

    private fun handleShareClick(action: RecordedLogsAction.ShareClick) {
        settingsRepository
            .flightRecorderData
            .data
            .find { it.id == action.item.id }
            ?.let { data ->
                viewModelScope.launch {
                    val result = fileManager.zipUriToCache(
                        uri = "${fileManager.logsDirectory}/${data.fileName}".toUri(),
                    )
                    sendAction(RecordedLogsAction.Internal.OnReceiveShareLogResult(result))
                }
            }
    }

    private fun handleInternalAction(action: RecordedLogsAction.Internal) {
        when (action) {
            is RecordedLogsAction.Internal.OnReceiveFlightRecorderData -> {
                handleOnReceiveFlightRecorderData(action)
            }

            is RecordedLogsAction.Internal.OnReceiveShareLogResult -> {
                handleOnReceiveShareLogResult(action)
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

    private fun handleOnReceiveShareLogResult(
        action: RecordedLogsAction.Internal.OnReceiveShareLogResult,
    ) {
        when (val result = action.result) {
            is ZipFileResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RecordedLogsState.DialogState.Error(
                            title = BitwardenString.unable_to_share.asText(),
                            message = BitwardenString.please_try_again_or_select_a_different_log
                                .asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            ZipFileResult.NothingToZip -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RecordedLogsState.DialogState.Error(
                            title = BitwardenString.unable_to_share.asText(),
                            message = BitwardenString
                                .the_log_file_you_are_trying_to_share_has_been_removed
                                .asText(),
                            error = null,
                        ),
                    )
                }
            }

            is ZipFileResult.Success -> {
                sendEvent(RecordedLogsEvent.ShareLog(uri = result.file.toString()))
            }
        }
    }
}

/**
 * Models the UI state for the recorded logs screen.
 */
@Parcelize
data class RecordedLogsState(
    val viewState: ViewState,
    val dialogState: DialogState?,
    val logsFolder: String,
) : Parcelable {
    /**
     * View states for the [RecordedLogsViewModel].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Indicates if the overflow items should be enabled.
         */
        abstract val isOverflowEnabled: Boolean

        /**
         * Represents the loading state for the [RecordedLogsViewModel].
         */
        @Parcelize
        data object Loading : ViewState() {
            override val isOverflowEnabled: Boolean get() = false
        }

        /**
         * Represents the empty state for the [RecordedLogsViewModel].
         */
        @Parcelize
        data object Empty : ViewState() {
            override val isOverflowEnabled: Boolean get() = false
        }

        /**
         * Represents the content state for the [RecordedLogsViewModel].
         */
        @Parcelize
        data class Content(
            val items: ImmutableList<DisplayItem>,
        ) : ViewState() {
            override val isOverflowEnabled: Boolean get() = true
        }
    }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    @Parcelize
    sealed class DialogState : Parcelable {
        /**
         * Displays a basic error dialog to the user.
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
            val error: Throwable?,
        ) : DialogState()
    }

    /**
     * Wrapper class for all displayable data in a row.
     */
    @Parcelize
    data class DisplayItem(
        val id: String,
        val title: Text,
        val subtextStart: Text,
        val subtextEnd: Text?,
        val isDeletedEnabled: Boolean,
    ) : Parcelable
}

/**
 * Models events for the recorded logs screen.
 */
sealed class RecordedLogsEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : RecordedLogsEvent()

    /**
     * Shares the logs are the given [uri].
     */
    data class ShareLog(val uri: String) : RecordedLogsEvent()

    /**
     * Displays a snackbar with the given [text].
     */
    data class ShowSnackbar(val text: Text) : RecordedLogsEvent()
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
     * Indicates that the user has dismissed a dialog.
     */
    data object DismissDialog : RecordedLogsAction()

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

        /**
         * Indicates that the file has been prepared.
         */
        data class OnReceiveShareLogResult(
            val result: ZipFileResult,
        ) : Internal()
    }
}
