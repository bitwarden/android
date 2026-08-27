package com.bitwarden.authenticator.ui.platform.feature.settings.export

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.authenticator.ui.platform.util.fileExtension
import com.bitwarden.core.data.util.toFormattedPattern
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.indicator.PasswordStrengthState
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import java.time.Clock
import javax.inject.Inject

/**
 * Manages state for the [ExportScreen].
 */
@Suppress("TooManyFunctions")
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

            is ExportAction.ConfirmFilePasswordInputChange -> {
                handleConfirmFilePasswordInputChange(action)
            }

            is ExportAction.DialogDismiss -> {
                handleDialogDismiss()
            }

            is ExportAction.ExportLocationReceive -> {
                handleExportLocationReceive(action)
            }

            is ExportAction.FilePasswordInputChange -> {
                handleFilePasswordInputChange(action)
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
            it.copy(
                exportVaultFormat = action.option,
                filePasswordInput = "",
                confirmFilePasswordInput = "",
                passwordStrengthState = PasswordStrengthState.NONE,
            )
        }
    }

    private fun handleConfirmExportClick() {
        if (state.exportVaultFormat == ExportVaultFormat.JSON_ENCRYPTED &&
            !validateFilePassword()
        ) {
            return
        }

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

    private fun handleConfirmFilePasswordInputChange(
        action: ExportAction.ConfirmFilePasswordInputChange,
    ) {
        mutableStateFlow.update { it.copy(confirmFilePasswordInput = action.input) }
    }

    private fun handleFilePasswordInputChange(action: ExportAction.FilePasswordInputChange) {
        mutableStateFlow.update { it.copy(filePasswordInput = action.input) }

        if (action.input.isEmpty()) {
            mutableStateFlow.update {
                it.copy(passwordStrengthState = PasswordStrengthState.NONE)
            }
            return
        }

        viewModelScope.launch {
            val result = authenticatorRepository.getPasswordStrength(password = action.input)
            sendAction(
                ExportAction.Internal.ReceivePasswordStrengthResult(
                    password = action.input,
                    result = result,
                ),
            )
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    /**
     * Shows an error and returns `false` when the file password cannot be used as entered.
     */
    private fun validateFilePassword(): Boolean {
        val message = when {
            state.filePasswordInput.isBlank() -> {
                BitwardenString.validation_field_required
                    .asText(BitwardenString.file_password.asText())
            }

            state.confirmFilePasswordInput.isBlank() -> {
                BitwardenString.validation_field_required
                    .asText(BitwardenString.confirm_file_password.asText())
            }

            state.filePasswordInput != state.confirmFilePasswordInput -> {
                BitwardenString.master_password_confirmation_val_message.asText()
            }

            else -> return true
        }
        mutableStateFlow.update {
            it.copy(dialogState = ExportState.DialogState.Error(message = message))
        }
        return false
    }

    private fun handleExportLocationReceive(action: ExportAction.ExportLocationReceive) {
        mutableStateFlow.update {
            it.copy(dialogState = ExportState.DialogState.Loading())
        }

        viewModelScope.launch {
            val result = authenticatorRepository.exportVaultData(
                format = state.exportVaultFormat,
                fileUri = action.fileUri,
                password = state
                    .filePasswordInput
                    .takeIf { state.exportVaultFormat == ExportVaultFormat.JSON_ENCRYPTED },
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

            is ExportAction.Internal.ReceivePasswordStrengthResult -> {
                handlePasswordStrengthResult(action)
            }
        }
    }

    private fun handlePasswordStrengthResult(
        action: ExportAction.Internal.ReceivePasswordStrengthResult,
    ) {
        // Results can land out of order; ignore any that no longer match the current input.
        if (action.password != state.filePasswordInput) return

        action.result
            .getOrNull()
            ?.toPasswordStrengthState()
            ?.let { strength ->
                mutableStateFlow.update { it.copy(passwordStrengthState = strength) }
            }
    }

    private fun handleExportDataToUriResult(result: ExportDataResult) {
        when (result) {
            ExportDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ExportState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.export_vault_failure.asText(),
                        ),
                    )
                }
            }

            is ExportDataResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        filePasswordInput = "",
                        confirmFilePasswordInput = "",
                        passwordStrengthState = PasswordStrengthState.NONE,
                    )
                }
                sendEvent(ExportEvent.ShowSnackBar(BitwardenString.export_success.asText()))
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
    val filePasswordInput: String = "",
    val confirmFilePasswordInput: String = "",
    val passwordStrengthState: PasswordStrengthState = PasswordStrengthState.NONE,
) {
    /**
     * Represents state of dialogs for the [ExportViewModel].
     */
    sealed class DialogState {
        /**
         * Displays a loading dialog with an optional [message].
         */
        data class Loading(
            val message: Text = BitwardenString.loading.asText(),
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
     * Display a Snackbar with the provided [message].
     */
    data class ShowSnackBar(val message: Text) : ExportEvent()

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
     * Indicates the user has changed the confirm file password input.
     */
    data class ConfirmFilePasswordInputChange(val input: String) : ExportAction()

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
     * Indicates the user has changed the file password input.
     */
    data class FilePasswordInputChange(val input: String) : ExportAction()

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

        /**
         * Indicates a password strength score has been received for [password].
         */
        data class ReceivePasswordStrengthResult(
            val password: String,
            val result: Result<UByte>,
        ) : Internal()
    }
}

/**
 * Maps an SDK strength level in the range `[0, 4]` to its [PasswordStrengthState].
 */
@Suppress("MagicNumber")
private fun UByte.toPasswordStrengthState(): PasswordStrengthState? = when (this.toInt()) {
    0 -> PasswordStrengthState.WEAK_1
    1 -> PasswordStrengthState.WEAK_2
    2 -> PasswordStrengthState.WEAK_3
    3 -> PasswordStrengthState.GOOD
    4 -> PasswordStrengthState.STRONG
    else -> null
}
