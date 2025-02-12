package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.authenticator.ui.platform.util.fileExtension
import com.bitwarden.authenticator.ui.platform.util.toFormattedPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import java.time.Clock
import javax.inject.Inject

/**
 * Manages state for the [ExportScreen].
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    private val clock: Clock,
) :
    BaseViewModel<ExportState, ExportEvent, ExportAction>(
        initialState = ExportState(dialogState = null, exportVaultFormat = ExportVaultFormat.JSON),
    ) {

    override fun handleAction(action: ExportAction) {
        when (action) {
            is ExportAction.CloseButtonClick -> {
                handleCloseButtonClick()
            }

            is ExportAction.ExportFormatOptionSelect -> {
                handleExportFormatOptionSelect(action)
            }

            is ExportAction.ConfirmExportClick -> {
                handleConfirmExportClick()
            }

            is ExportAction.DialogDismiss -> {
                handleDialogDismiss()
            }

            is ExportAction.ExportLocationReceive -> {
                handleExportLocationReceive(action)
            }

            is ExportAction.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleCloseButtonClick() {
        sendEvent(ExportEvent.NavigateBack)
    }

    private fun handleExportFormatOptionSelect(action: ExportAction.ExportFormatOptionSelect) {
        mutableStateFlow.update {
            it.copy(exportVaultFormat = action.option)
        }
    }

    private fun handleConfirmExportClick() {

        val date = clock.instant().toFormattedPattern(
            pattern = "yyyyMMddHHmmss",
            clock = clock,
        )
        val extension = state.exportVaultFormat.fileExtension
        val fileName = "authenticator_export_$date.$extension"

        sendEvent(
            ExportEvent.NavigateToSelectExportDestination(fileName),
        )
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    private fun handleExportLocationReceive(action: ExportAction.ExportLocationReceive) {
        mutableStateFlow.update {
            it.copy(dialogState = ExportState.DialogState.Loading())
        }

        viewModelScope.launch {
            val result = authenticatorRepository.exportVaultData(
                format = state.exportVaultFormat,
                fileUri = action.fileUri,
            )

            sendAction(
                ExportAction.Internal.SaveExportDataToUriResultReceive(
                    result = result,
                ),
            )
        }
    }

    private fun handleInternalAction(action: ExportAction.Internal) {
        when (action) {
            is ExportAction.Internal.SaveExportDataToUriResultReceive -> {
                handleExportDataToUriResult(action.result)
            }
        }
    }

    private fun handleExportDataToUriResult(result: ExportDataResult) {
        when (result) {
            ExportDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ExportState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.export_vault_failure.asText(),
                        ),
                    )
                }
            }

            is ExportDataResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(ExportEvent.ShowToast(R.string.export_success.asText()))
            }
        }
    }
}

/**
 * Represents the state of the [ExportViewModel].
 */
data class ExportState(
    @IgnoredOnParcel
    val exportData: String? = null,
    val dialogState: DialogState? = null,
    val exportVaultFormat: ExportVaultFormat,
) {
    /**
     * Represents state of dialogs for the [ExportViewModel].
     */
    sealed class DialogState {
        /**
         * Displays a loading dialog with an optional [message].
         */
        data class Loading(
            val message: Text = R.string.loading.asText(),
        ) : DialogState()

        /**
         * Displays an error dialog with an optional [title], and a [message].
         */
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Represents events for the [ExportViewModel].
 */
sealed class ExportEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : ExportEvent()

    /**
     * Display a toast with the provided [message].
     */
    data class ShowToast(val message: Text) : ExportEvent()

    /**
     * Navigate to the select export destination screen.
     */
    data class NavigateToSelectExportDestination(val fileName: String) : ExportEvent()
}

/**
 * Represents actions for the [ExportViewModel].
 */
sealed class ExportAction {

    /**
     * Indicates the user has clicked the close button.
     */
    data object CloseButtonClick : ExportAction()

    /**
     * Indicates the user has clicked the export confirmation button.
     */
    data object ConfirmExportClick : ExportAction()

    /**
     * Indicates the user has dismissed the dialog.
     */
    data object DialogDismiss : ExportAction()

    /**
     * Indicates the user has selected an export format.
     */
    data class ExportFormatOptionSelect(val option: ExportVaultFormat) : ExportAction()

    /**
     * Indicates the user has selected a location for the exported data.
     */
    data class ExportLocationReceive(val fileUri: Uri) : ExportAction()

    /**
     * Represents actions the [ExportViewModel] itself may trigger.
     */
    sealed class Internal : ExportAction() {

        /**
         * Indicates the result for saving exported data to a URI has been received.
         */
        data class SaveExportDataToUriResultReceive(
            val result: ExportDataResult,
        ) : Internal()
    }
}
