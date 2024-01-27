package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Export Vault screen.
 */
@HiltViewModel
class ExportVaultViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ExportVaultState, ExportVaultEvent, ExportVaultAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ExportVaultState(
            dialogState = null,
            exportFormat = ExportVaultFormat.JSON,
            passwordInput = "",
        ),
) {
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
            ExportVaultAction.DialogDismiss -> handleDialogDismiss()
            is ExportVaultAction.ExportFormatOptionSelect -> handleExportFormatOptionSelect(action)
            is ExportVaultAction.PasswordInputChanged -> handlePasswordInputChanged(action)

            is ExportVaultAction.Internal.ReceiveValidatePasswordResult -> {
                handleReceiveValidatePasswordResult(action)
            }
        }
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
        if (mutableStateFlow.value.passwordInput.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.validation_field_required.asText(
                            R.string.master_password.asText(),
                        ),
                    ),
                )
            }
            return
        }

        // Otherwise, validate the password.
        viewModelScope.launch {
            sendAction(
                ExportVaultAction.Internal.ReceiveValidatePasswordResult(
                    isPasswordValid = settingsRepository.validatePassword(
                        password = mutableStateFlow.value.passwordInput,
                    ),
                ),
            )
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
     * Update the state with the new password input.
     */
    private fun handlePasswordInputChanged(action: ExportVaultAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(passwordInput = action.input)
        }
    }

    /**
     * Show an alert or proceed to export the vault after validating the password.
     */
    private fun handleReceiveValidatePasswordResult(
        action: ExportVaultAction.Internal.ReceiveValidatePasswordResult,
    ) {
        // Display an error dialog if the password is invalid.
        if (!action.isPasswordValid) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = ExportVaultState.DialogState.Error(
                        title = null,
                        message = R.string.invalid_master_password.asText(),
                    ),
                )
            }
        } else {
            // TODO: BIT-1274, BIT-1275, and BIT-1276
            sendEvent(ExportVaultEvent.ShowToast("Not yet implemented".asText()))
        }
    }
}

/**
 * Models state of the Export Vault screen.
 */
@Parcelize
data class ExportVaultState(
    val dialogState: DialogState?,
    val exportFormat: ExportVaultFormat,
    val passwordInput: String,
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
            val message: Text,
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
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : ExportVaultAction()

    /**
     * Indicates that an export format option was selected.
     */
    data class ExportFormatOptionSelect(val option: ExportVaultFormat) : ExportVaultAction()

    /**
     * Indicates that the password input has changed.
     */
    data class PasswordInputChanged(val input: String) : ExportVaultAction()

    /**
     * Models actions that the [ExportVaultViewModel] might send itself.
     */
    sealed class Internal : ExportVaultAction() {
        /**
         * Indicates that a validate password result has been received.
         */
        data class ReceiveValidatePasswordResult(
            val isPasswordValid: Boolean,
        ) : Internal()
    }
}
