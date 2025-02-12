package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import javax.inject.Inject

/**
 * View model for the Importing screen.
 */
@HiltViewModel
class ImportingViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
) :
    BaseViewModel<ImportState, ImportEvent, ImportAction>(
        initialState = ImportState(importFileFormat = ImportFileFormat.BITWARDEN_JSON),
    ) {

    override fun handleAction(action: ImportAction) {
        when (action) {
            ImportAction.CloseButtonClick -> {
                handleCloseButtonClick()
            }

            ImportAction.ImportClick -> {
                handleConfirmImportClick()
            }

            ImportAction.DialogDismiss -> {
                handleDialogDismiss()
            }

            is ImportAction.ImportFormatOptionSelect -> {
                handleImportFormatOptionSelect(action)
            }

            is ImportAction.ImportLocationReceive -> {
                handleImportLocationReceive(action)
            }

            is ImportAction.Internal -> {
                handleInternalAction(action)
            }
        }
    }

    private fun handleCloseButtonClick() {
        sendEvent(ImportEvent.NavigateBack)
    }

    private fun handleConfirmImportClick() {
        sendEvent(ImportEvent.NavigateToSelectImportFile(state.importFileFormat))
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleImportFormatOptionSelect(action: ImportAction.ImportFormatOptionSelect) {
        mutableStateFlow.update { it.copy(importFileFormat = action.option) }
    }

    private fun handleImportLocationReceive(action: ImportAction.ImportLocationReceive) {
        mutableStateFlow.update { it.copy(dialogState = ImportState.DialogState.Loading()) }

        viewModelScope.launch {
            val result = authenticatorRepository.importVaultData(
                format = state.importFileFormat,
                fileData = action.fileUri,
            )

            sendAction(
                ImportAction.Internal.SaveImportDataToUriResultReceive(result),
            )
        }
    }

    private fun handleInternalAction(action: ImportAction.Internal) {
        when (action) {
            is ImportAction.Internal.SaveImportDataToUriResultReceive -> {
                handleSaveImportDataToUriResultReceive(action.result)
            }
        }
    }

    private fun handleSaveImportDataToUriResultReceive(result: ImportDataResult) {
        when (result) {
            is ImportDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ImportState.DialogState.Error(
                            title = result.title ?: R.string.an_error_has_occurred.asText(),
                            message = result.message ?: R.string.import_vault_failure.asText(),
                        ),
                    )
                }
            }

            ImportDataResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    ImportEvent.ShowToast(
                        message = R.string.import_success.asText(),
                    ),
                )
                sendEvent(ImportEvent.NavigateBack)
            }
        }
    }
}

/**
 * Represents state for the [ImportingScreen].
 */
data class ImportState(
    @IgnoredOnParcel
    val fileUri: Uri? = null,
    val dialogState: DialogState? = null,
    val importFileFormat: ImportFileFormat,
) {

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState {

        /**
         * Represents a loading dialog with the given [message].
         */
        data class Loading(
            val message: Text = R.string.loading.asText(),
        ) : DialogState()

        /**
         * Represents a dismissible dialog with the given error [title] and [message].
         */
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the [ImportingScreen].
 */
sealed class ImportEvent {

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : ImportEvent()

    /**
     * Show a Toast with the given [message].
     */
    data class ShowToast(val message: Text) : ImportEvent()

    /**
     * Navigate to the select import file screen.
     */
    data class NavigateToSelectImportFile(val importFileFormat: ImportFileFormat) : ImportEvent()
}

/**
 * Models actions for the [ImportingScreen].
 */
sealed class ImportAction {

    /**
     * Indicates the user clicked close.
     */
    data object CloseButtonClick : ImportAction()

    /**
     * Indicates the user clicked import.
     */
    data object ImportClick : ImportAction()

    /**
     * Indicates the user dismissed the dialog.
     */
    data object DialogDismiss : ImportAction()

    /**
     * Indicates the user selected and import file format.
     */
    data class ImportFormatOptionSelect(val option: ImportFileFormat) : ImportAction()

    /**
     * Indicates the user selected a file to import.
     */
    data class ImportLocationReceive(val fileUri: IntentManager.FileData) : ImportAction()

    /**
     * Models actions the [ImportingScreen] itself may send.
     */
    sealed class Internal : ImportAction() {

        /**
         * Indicates the save data result has been received.
         */
        data class SaveImportDataToUriResultReceive(
            val result: ImportDataResult,
        ) : Internal()
    }
}
