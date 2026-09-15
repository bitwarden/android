package com.x8bit.bitwarden.ui.platform.feature.biometrics

import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.crypto.Cipher
import javax.inject.Inject

/**
 * ViewModel for the Update Biometrics screen.
 */
@HiltViewModel
class UpdateBiometricsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<UpdateBiometricsState, UpdateBiometricsEvent, UpdateBiometricsAction>(
    initialState = UpdateBiometricsState(
        dialogState = null,
    ),
) {
    override fun handleAction(action: UpdateBiometricsAction) {
        when (action) {
            is UpdateBiometricsAction.BiometricCipherReceived -> {
                handleBiometricCipherReceived(action)
            }

            UpdateBiometricsAction.ConfirmBiometricsClick -> handleConfirmBiometricClick()
            UpdateBiometricsAction.ConfirmTurnOffClick -> handleConfirmTurnOffClick()
            UpdateBiometricsAction.DismissDialog -> handleDismissDialog()
            UpdateBiometricsAction.TurnOffBiometricsClick -> handleTurnOffBiometricsClick()
            is UpdateBiometricsAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: UpdateBiometricsAction.Internal) {
        when (action) {
            is UpdateBiometricsAction.Internal.BiometricsKeyResultReceive -> {
                handleBiometricsKeyResultReceive(action)
            }
        }
    }

    private fun handleBiometricsKeyResultReceive(
        action: UpdateBiometricsAction.Internal.BiometricsKeyResultReceive,
    ) {
        when (action.result) {
            is BiometricsKeyResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = UpdateBiometricsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            BiometricsKeyResult.Success -> {
                Timber.i("Rotated Biometrics Unlock Key")
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(UpdateBiometricsEvent.NavigateBack)
            }
        }
    }

    private fun handleBiometricCipherReceived(
        action: UpdateBiometricsAction.BiometricCipherReceived,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = UpdateBiometricsState.DialogState.Loading(
                    message = BitwardenString.saving.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = settingsRepository.setupBiometricsKey(cipher = action.cipher)
            sendAction(UpdateBiometricsAction.Internal.BiometricsKeyResultReceive(result = result))
        }
    }

    private fun handleConfirmBiometricClick() {
        authRepository
            .activeUserId
            ?.let {
                authRepository.clearBiometrics(userId = it)
                authRepository.createCipherOrNull(userId = it)
            }
            ?.let { sendEvent(UpdateBiometricsEvent.ShowBiometricsPrompt(cipher = it)) }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = UpdateBiometricsState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }
    }

    private fun handleConfirmTurnOffClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        authRepository.userStateFlow.value?.let {
            // Clear Biometrics
            authRepository.clearBiometrics(userId = it.activeUserId)
            val hasOtherUnlockMechanism = it.activeAccount.hasMasterPassword ||
                it.activeAccount.vaultUnlockType == VaultUnlockType.PIN
            if (!hasOtherUnlockMechanism) {
                settingsRepository.vaultTimeoutAction = VaultTimeoutAction.LOGOUT
            }
        }
        sendEvent(UpdateBiometricsEvent.NavigateBack)
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleTurnOffBiometricsClick() {
        mutableStateFlow.update {
            it.copy(dialogState = UpdateBiometricsState.DialogState.DisableBiometricsAndClose)
        }
    }
}

/**
 * State for the Update Biometrics screen.
 */
data class UpdateBiometricsState(
    val dialogState: DialogState?,
) {
    /**
     * Represents the dialogs available to this screen.
     */
    sealed class DialogState {
        /**
         * Represents a dialog informing the user of the ramifications of skipping this process.
         */
        data object DisableBiometricsAndClose : DialogState()

        /**
         * Represents an error dialog with an optional title and message.
         */
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with a message.
         */
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Events for the Update Biometrics screen.
 */
sealed class UpdateBiometricsEvent {
    /**
     * Navigate away from this screen.
     */
    data object NavigateBack : UpdateBiometricsEvent()

    /**
     * Shows the prompt for Biometrics using with the given [cipher].
     */
    data class ShowBiometricsPrompt(val cipher: Cipher) : UpdateBiometricsEvent()
}

/**
 * Actions for the Update Biometrics screen.
 */
sealed class UpdateBiometricsAction {
    /**
     * The biometric cipher has been received
     */
    data class BiometricCipherReceived(val cipher: Cipher) : UpdateBiometricsAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialog : UpdateBiometricsAction()

    /**
     * The user has clicked the confirm button.
     */
    data object ConfirmBiometricsClick : UpdateBiometricsAction()

    /**
     * The user has clicked to confirm that they want to turn off Biometrics.
     */
    data object ConfirmTurnOffClick : UpdateBiometricsAction()

    /**
     * The user has clicked the turn off button.
     */
    data object TurnOffBiometricsClick : UpdateBiometricsAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : UpdateBiometricsAction() {
        /**
         * Indicates the biometrics key validation results has been received.
         */
        data class BiometricsKeyResultReceive(val result: BiometricsKeyResult) : Internal()
    }
}
