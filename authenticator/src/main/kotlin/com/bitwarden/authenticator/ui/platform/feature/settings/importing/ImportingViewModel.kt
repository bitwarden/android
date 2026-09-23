package com.bitwarden.authenticator.ui.platform.feature.settings.importing

import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.model.FileData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import javax.inject.Inject

/**
 * View model for the Importing screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ImportingViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<ImportState, ImportEvent, ImportAction>(
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

            ImportAction.PasswordPromptDismiss -> {
                handlePasswordPromptDismiss()
            }

            is ImportAction.PasswordSubmit -> {
                handlePasswordSubmit(action)
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
        mutableStateFlow.update {
            it.copy(fileData = action.fileUri, dialogState = ImportState.DialogState.Loading())
        }
        importVaultData(fileData = action.fileUri, password = null)
    }

    private fun handlePasswordPromptDismiss() {
        mutableStateFlow.update { it.copy(fileData = null, dialogState = null) }
    }

    private fun handlePasswordSubmit(action: ImportAction.PasswordSubmit) {
        val fileData = state.fileData ?: return
        mutableStateFlow.update { it.copy(dialogState = ImportState.DialogState.Loading()) }
        importVaultData(fileData = fileData, password = action.password)
    }

    private fun importVaultData(fileData: FileData, password: String?) {
        viewModelScope.launch {
            val result = authenticatorRepository.importVaultData(
                format = state.importFileFormat,
                fileData = fileData,
                password = password,
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
            ImportDataResult.PasswordRequired -> {
                mutableStateFlow.update {
                    it.copy(dialogState = ImportState.DialogState.PasswordPrompt())
                }
            }

            ImportDataResult.IncorrectPassword -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ImportState.DialogState.PasswordPrompt(
                            errorMessage = BitwardenString
                                .the_password_you_entered_is_incorrect
                                .asText(),
                        ),
                    )
                }
            }

            is ImportDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        fileData = null,
                        dialogState = ImportState.DialogState.Error(
                            title = result.title ?: BitwardenString.an_error_has_occurred.asText(),
                            message = result.message
                                ?: BitwardenString.import_vault_failure.asText(),
                        ),
                    )
                }
            }

            ImportDataResult.Success -> {
                mutableStateFlow.update { it.copy(fileData = null, dialogState = null) }
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = BitwardenString.import_success.asText()),
                    relay = SnackbarRelay.IMPORT_SUCCESS,
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
    val fileData: FileData? = null,
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
            val message: Text = BitwardenString.loading.asText(),
        ) : DialogState()

        /**
         * Represents a dismissible dialog with the given error [title] and [message].
         */
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a prompt for the password of a password-protected file, optionally showing an
         * [errorMessage] from a previous incorrect attempt.
         */
        data class PasswordPrompt(
            val errorMessage: Text? = null,
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
    data class ImportLocationReceive(val fileUri: FileData) : ImportAction()

    /**
     * Indicates the user dismissed the password prompt.
     */
    data object PasswordPromptDismiss : ImportAction()

    /**
     * Indicates the user submitted a [password] for a password-protected file.
     */
    data class PasswordSubmit(val password: String) : ImportAction()

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
