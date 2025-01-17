package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifyOtpResult
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ExportVaultDataResult
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.toExportFormat
import com.x8bit.bitwarden.ui.platform.util.fileExtension
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Export Vault screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ExportVaultViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    policyManager: PolicyManager,
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val fileManager: FileManager,
    private val clock: Clock,
) : BaseViewModel<ExportVaultState, ExportVaultEvent, ExportVaultAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ExportVaultState(
            confirmFilePasswordInput = "",
            dialogState = null,
            email = requireNotNull(authRepository.userStateFlow.value?.activeAccount?.email),
            exportData = null,
            exportFormat = ExportVaultFormat.JSON,
            filePasswordInput = "",
            passwordInput = "",
            passwordStrengthState = PasswordStrengthState.NONE,
            policyPreventsExport = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT)
                .any(),
            showSendCodeButton = authRepository
                .userStateFlow
                .value
                ?.activeAccount
                ?.hasMasterPassword == false,
        ),
) {
    /**
     * Keeps track of async request to get password strength. Should be cancelled
     * when user input changes.
     */
    private var passwordStrengthJob: Job = Job().apply { complete() }

    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ExportVaultAction) {
        when (action) {
            ExportVaultAction.CloseButtonClick -> handleCloseButtonClicked()
            ExportVaultAction.ConfirmExportVaultClicked -> handleConfirmExportVaultClicked()
            is ExportVaultAction.ConfirmFilePasswordInputChange -> {
                handleConfirmFilePasswordInputChanged(action)
            }

            ExportVaultAction.DialogDismiss -> handleDialogDismiss()
            is ExportVaultAction.FilePasswordInputChange -> handleFilePasswordInputChanged(action)
            is ExportVaultAction.ExportFormatOptionSelect -> handleExportFormatOptionSelect(action)
            is ExportVaultAction.PasswordInputChanged -> handlePasswordInputChanged(action)
            ExportVaultAction.SendCodeClick -> handleSendCodeClick()
            is ExportVaultAction.ExportLocationReceive -> handleExportLocationReceive(action)

            is ExportVaultAction.Internal.ReceiveValidatePasswordResult -> {
                handleReceiveValidatePasswordResult(action)
            }

            is ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult -> {
                handleReceivePrepareVaultDataResult(action)
            }

            is ExportVaultAction.Internal.ReceivePasswordStrengthResult -> {
                handleReceivePasswordStrengthResult(action)
            }

            is ExportVaultAction.Internal.SaveExportDataToUriResultReceive -> {
                handleExportDataFinishedSavingToDisk(action)
            }

            is ExportVaultAction.Internal.ReceiveVerifyOneTimePasscodeResult -> {
                handleReceiveVerifyOneTimePasscodeResult(action)
            }

            is ExportVaultAction.Internal.OtpCodeResult -> handleOtpCodeResult(action)
        }
    }

    private fun handleOtpCodeResult(action: ExportVaultAction.Internal.OtpCodeResult) {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
        val toastMessage = when (val result = action.result) {
            is RequestOtpResult.Error -> {
                result.message?.asText() ?: R.string.generic_error_message.asText()
            }

            RequestOtpResult.Success -> R.string.code_sent.asText()
        }
        sendEvent(ExportVaultEvent.ShowToast(message = toastMessage))
    }

    /**
     * Dismiss the view.
     */
    private fun handleCloseButtonClicked() {
        sendEvent(ExportVaultEvent.NavigateBack)
    }

    /**
     * Verify the master password after confirming exporting the vault.
     */
    private fun handleConfirmExportVaultClicked() {
        // Display an error alert if the user hasn't entered a password.
        if (state.passwordInput.isBlank()) {
            updateStateWithError(
                message = R.string.validation_field_required.asText(
                    R.string.master_password.asText(),
                ),
            )
            return
        } else if (state.exportFormat == ExportVaultFormat.JSON_ENCRYPTED) {
            when {
                state.filePasswordInput.isBlank() -> {
                    updateStateWithError(
                        message = R.string.validation_field_required
                            .asText(R.string.file_password.asText()),
                    )
                    return
                }

                state.confirmFilePasswordInput.isBlank() -> {
                    updateStateWithError(
                        message = R.string.validation_field_required
                            .asText(R.string.confirm_file_password.asText()),
                    )
                    return
                }

                state.filePasswordInput != state.confirmFilePasswordInput -> {
                    updateStateWithError(
                        message = R.string.master_password_confirmation_val_message.asText(),
                    )
                    return
                }
            }
        }

        // Otherwise, validate the password.
        viewModelScope.launch {
            sendAction(
                if (state.showSendCodeButton) {
                    ExportVaultAction.Internal.ReceiveVerifyOneTimePasscodeResult(
                        result = authRepository.verifyOneTimePasscode(
                            oneTimePasscode = state.passwordInput,
                        ),
                    )
                } else {
                    ExportVaultAction.Internal.ReceiveValidatePasswordResult(
                        result = authRepository.validatePassword(
                            password = state.passwordInput,
                        ),
                    )
                },
            )
        }
    }

    /**
     * Update the state with the new confirm file password input.
     */
    private fun handleConfirmFilePasswordInputChanged(
        action: ExportVaultAction.ConfirmFilePasswordInputChange,
    ) {
        mutableStateFlow.update {
            it.copy(confirmFilePasswordInput = action.input)
        }
    }

    /**
     * Dismiss the dialog.
     */
    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    /**
     * Update the state with the selected export format.
     */
    private fun handleExportFormatOptionSelect(action: ExportVaultAction.ExportFormatOptionSelect) {
        mutableStateFlow.update {
            it.copy(exportFormat = action.option)
        }
    }

    /**
     * Save the vault data in the location.
     */
    private fun handleExportLocationReceive(action: ExportVaultAction.ExportLocationReceive) {
        val exportData = state.exportData
        if (exportData == null) {
            updateStateWithError(R.string.export_vault_failure.asText())
            return
        }

        viewModelScope.launch {
            val result = fileManager
                .stringToUri(
                    fileUri = action.fileUri,
                    dataString = exportData,
                )

            sendAction(ExportVaultAction.Internal.SaveExportDataToUriResultReceive(result))
        }
    }

    /**
     * Update the state with the new file password input.
     */
    private fun handleFilePasswordInputChanged(action: ExportVaultAction.FilePasswordInputChange) {
        mutableStateFlow.update {
            it.copy(filePasswordInput = action.input)
        }
        // Update password strength
        passwordStrengthJob.cancel()
        if (action.input.isEmpty()) {
            mutableStateFlow.update {
                it.copy(passwordStrengthState = PasswordStrengthState.NONE)
            }
        } else {
            passwordStrengthJob = viewModelScope.launch {
                val result = authRepository.getPasswordStrength(
                    email = state.email,
                    password = action.input,
                )
                trySendAction(ExportVaultAction.Internal.ReceivePasswordStrengthResult(result))
            }
        }
    }

    /**
     * Update the state with the new password input.
     */
    private fun handlePasswordInputChanged(action: ExportVaultAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleSendCodeClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExportVaultState.DialogState.Loading(
                    message = R.string.sending.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                ExportVaultAction.Internal.OtpCodeResult(
                    result = authRepository.requestOneTimePasscode(),
                ),
            )
        }
    }

    /**
     * Show an alert or proceed to export the vault after validating the password.
     */
    private fun handleReceiveValidatePasswordResult(
        action: ExportVaultAction.Internal.ReceiveValidatePasswordResult,
    ) {
        when (action.result) {
            ValidatePasswordResult.Error -> {
                updateStateWithError(R.string.generic_error_message.asText())
            }

            is ValidatePasswordResult.Success -> {
                // Display an error dialog if the password is invalid.
                if (!action.result.isValid) {
                    updateStateWithError(R.string.invalid_master_password.asText())
                    return
                }

                exportVaultData()
            }
        }
    }

    /**
     * Show an error message or proceed to export the vault after receiving the data.
     */
    private fun handleReceivePrepareVaultDataResult(
        action: ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult,
    ) {
        when (val result = action.result) {
            is ExportVaultDataResult.Error -> {
                updateStateWithError(
                    message = R.string.export_vault_failure.asText(),
                )
            }

            is ExportVaultDataResult.Success -> {
                val date = clock.instant().toFormattedPattern(
                    pattern = "yyyyMMddHHmmss",
                    clock = clock,
                )
                val extension = state.exportFormat.fileExtension
                val fileName = "bitwarden_export_$date.$extension"

                mutableStateFlow.update {
                    it.copy(
                        confirmFilePasswordInput = "",
                        dialogState = null,
                        filePasswordInput = "",
                        passwordInput = "",
                        passwordStrengthState = PasswordStrengthState.NONE,
                        exportData = result.vaultData,
                    )
                }

                sendEvent(
                    ExportVaultEvent.NavigateToSelectExportDataLocation(fileName),
                )
            }
        }
    }

    private fun handleReceivePasswordStrengthResult(
        action: ExportVaultAction.Internal.ReceivePasswordStrengthResult,
    ) {
        when (val result = action.result) {
            is PasswordStrengthResult.Success -> {
                val updatedState = when (result.passwordStrength) {
                    PasswordStrength.LEVEL_0 -> PasswordStrengthState.WEAK_1
                    PasswordStrength.LEVEL_1 -> PasswordStrengthState.WEAK_2
                    PasswordStrength.LEVEL_2 -> PasswordStrengthState.WEAK_3
                    PasswordStrength.LEVEL_3 -> PasswordStrengthState.GOOD
                    PasswordStrength.LEVEL_4 -> PasswordStrengthState.STRONG
                }
                mutableStateFlow.update { oldState ->
                    oldState.copy(
                        passwordStrengthState = updatedState,
                    )
                }
            }

            PasswordStrengthResult.Error -> {
                // Leave UI the same
            }
        }
    }

    private fun handleExportDataFinishedSavingToDisk(
        action: ExportVaultAction.Internal.SaveExportDataToUriResultReceive,
    ) {
        if (!action.result) {
            updateStateWithError(R.string.export_vault_failure.asText())
            return
        }

        sendEvent(ExportVaultEvent.ShowToast(R.string.export_vault_success.asText()))
    }

    private fun handleReceiveVerifyOneTimePasscodeResult(
        action: ExportVaultAction.Internal.ReceiveVerifyOneTimePasscodeResult,
    ) {
        when (action.result) {
            VerifyOtpResult.Verified -> exportVaultData()

            is VerifyOtpResult.NotVerified -> {
                updateStateWithError(R.string.generic_error_message.asText())
            }
        }
    }

    /**
     * Handles exporting the vault data after all validation has finished.
     */
    private fun exportVaultData() {
        mutableStateFlow.update {
            it.copy(dialogState = ExportVaultState.DialogState.Loading())
        }

        viewModelScope.launch {
            val result = vaultRepository.exportVaultDataToString(
                format = state.exportFormat.toExportFormat(
                    password = if (state.exportFormat == ExportVaultFormat.JSON_ENCRYPTED) {
                        state.filePasswordInput
                    } else {
                        state.passwordInput
                    },
                ),
            )

            sendAction(
                ExportVaultAction.Internal.ReceiveExportVaultDataToStringResult(
                    result = result,
                ),
            )
        }
    }

    private fun updateStateWithError(message: Text) {
        mutableStateFlow.update {
            it.copy(
                dialogState = ExportVaultState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = message,
                ),
            )
        }
    }
}

/**
 * Models state of the Export Vault screen.
 */
@Parcelize
data class ExportVaultState(
    @IgnoredOnParcel
    val exportData: String? = null,
    val confirmFilePasswordInput: String,
    val dialogState: DialogState?,
    val email: String,
    val exportFormat: ExportVaultFormat,
    val filePasswordInput: String,
    val passwordInput: String,
    val passwordStrengthState: PasswordStrengthState,
    val policyPreventsExport: Boolean,
    val showSendCodeButton: Boolean,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. If no title
         * is specified a default will be provided.
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text = R.string.loading.asText(),
        ) : DialogState()
    }
}

/**
 * Models events for the Export Vault screen.
 */
sealed class ExportVaultEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : ExportVaultEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: Text) : ExportVaultEvent()

    /**
     *  Navigates to select a location where to save the vault data with the [fileName].
     */
    data class NavigateToSelectExportDataLocation(val fileName: String) : ExportVaultEvent()
}

/**
 * Models actions for the Export Vault screen.
 */
sealed class ExportVaultAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : ExportVaultAction()

    /**
     * Indicates that the confirm export vault button was clicked.
     */
    data object ConfirmExportVaultClicked : ExportVaultAction()

    /**
     * Indicates that the confirm file password input has changed.
     */
    data class ConfirmFilePasswordInputChange(val input: String) : ExportVaultAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : ExportVaultAction()

    /**
     * Indicates that an export format option was selected.
     */
    data class ExportFormatOptionSelect(val option: ExportVaultFormat) : ExportVaultAction()

    /**
     * Indicates the user has selected a location to save the file.
     */
    data class ExportLocationReceive(
        val fileUri: Uri,
    ) : ExportVaultAction()

    /**
     * Indicates that the file password input has changed.
     */
    data class FilePasswordInputChange(val input: String) : ExportVaultAction()

    /**
     * Indicates that the password input has changed.
     */
    data class PasswordInputChanged(val input: String) : ExportVaultAction()

    /**
     * Indicates that the user pressed the button to send a code in place of entering a password.
     */
    data object SendCodeClick : ExportVaultAction()

    /**
     * Models actions that the [ExportVaultViewModel] might send itself.
     */
    sealed class Internal : ExportVaultAction() {

        /**
         * Indicates that the item has finished saving to disk.
         */
        data class SaveExportDataToUriResultReceive(
            val result: Boolean,
        ) : Internal()

        /**
         * Indicates that the result for exporting the vault data has been received.
         */
        data class ReceiveExportVaultDataToStringResult(
            val result: ExportVaultDataResult,
        ) : Internal()

        /**
         * Indicates that the result for getting the password strength has been received.
         */
        data class ReceivePasswordStrengthResult(
            val result: PasswordStrengthResult,
        ) : Internal()

        /**
         * Indicates that a validate password result has been received.
         */
        data class ReceiveValidatePasswordResult(
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a result for verifying the one-time passcode has been received.
         */
        data class ReceiveVerifyOneTimePasscodeResult(
            val result: VerifyOtpResult,
        ) : Internal()

        /**
         * Indicates that a result for requesting the one-time passcode has been received.
         */
        data class OtpCodeResult(val result: RequestOtpResult) : Internal()
    }
}
