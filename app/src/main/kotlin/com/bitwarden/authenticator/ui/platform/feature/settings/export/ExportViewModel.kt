package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportFormat
import com.bitwarden.authenticator.ui.platform.util.fileExtension
import com.bitwarden.authenticator.ui.platform.util.toFormattedPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    private val clock: Clock,
) :
    BaseViewModel<ExportState, ExportEvent, ExportAction>(
        initialState = ExportState(dialogState = null, exportFormat = ExportFormat.JSON)
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
            it.copy(exportFormat = action.option)
        }
    }

    private fun handleConfirmExportClick() {

        val date = clock.instant().toFormattedPattern(
            pattern = "yyyyMMddHHmmss",
            clock = clock,
        )
        val extension = state.exportFormat.fileExtension
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
                format = state.exportFormat,
                fileUri = action.fileUri,
            )

            sendAction(
                ExportAction.Internal.SaveExportDataToUriResultReceive(
                    result = result
                )
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

data class ExportState(
    @IgnoredOnParcel
    val exportData: String? = null,
    val dialogState: DialogState? = null,
    val exportFormat: ExportFormat,
) {
    sealed class DialogState {
        data class Loading(
            val message: Text = R.string.loading.asText(),
        ) : DialogState()

        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()
    }
}

sealed class ExportEvent {
    data object NavigateBack : ExportEvent()

    data class ShowToast(val message: Text) : ExportEvent()

    data class NavigateToSelectExportDestination(val fileName: String) : ExportEvent()
}

sealed class ExportAction {
    data object CloseButtonClick : ExportAction()

    data object ConfirmExportClick : ExportAction()

    data object DialogDismiss : ExportAction()

    data class ExportFormatOptionSelect(val option: ExportFormat) : ExportAction()

    data class ExportLocationReceive(val fileUri: Uri) : ExportAction()

    sealed class Internal : ExportAction() {

        data class SaveExportDataToUriResultReceive(
            val result: ExportDataResult,
        ) : Internal()
    }
}
