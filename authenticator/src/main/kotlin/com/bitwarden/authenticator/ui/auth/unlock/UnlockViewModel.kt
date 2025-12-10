package com.bitwarden.authenticator.ui.auth.unlock

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.crypto.Cipher
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the Unlock screen.
 */
@HiltViewModel
class UnlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : BaseViewModel<UnlockState, UnlockEvent, UnlockAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        UnlockState(
            isBiometricsEnabled = authRepository.isUnlockWithBiometricsEnabled,
            isBiometricsValid = authRepository.isBiometricIntegrityValid(),
            showBiometricInvalidatedMessage = false,
            dialog = null,
        )
    },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        authRepository.getOrCreateCipher()?.let {
            sendEvent(UnlockEvent.PromptForBiometrics(cipher = it))
        }
    }

    override fun handleAction(action: UnlockAction) {
        when (action) {
            is UnlockAction.BiometricsUnlockSuccess -> handleBiometricsUnlockSuccess(action)
            UnlockAction.DismissDialog -> handleDismissDialog()
            UnlockAction.BiometricsLockout -> handleBiometricsLockout()
            UnlockAction.BiometricsUnlockClick -> handleBiometricsUnlockClick()
            is UnlockAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: UnlockAction.Internal) {
        when (action) {
            is UnlockAction.Internal.ReceiveVaultUnlockResult -> {
                handleReceiveVaultUnlockResult(action)
            }
        }
    }

    private fun handleReceiveVaultUnlockResult(
        action: UnlockAction.Internal.ReceiveVaultUnlockResult,
    ) {
        when (val result = action.vaultUnlockResult) {
            is BiometricsUnlockResult.BiometricDecodingError -> {
                authRepository.clearBiometrics()
                mutableStateFlow.update {
                    it.copy(
                        isBiometricsValid = false,
                        dialog = UnlockState.Dialog.Error(
                            title = BitwardenString.biometrics_failed.asText(),
                            message = BitwardenString.biometrics_decoding_failure.asText(),
                        ),
                    )
                }
            }

            is BiometricsUnlockResult.InvalidStateError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = UnlockState.Dialog.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            BiometricsUnlockResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                sendEvent(UnlockEvent.NavigateToItemListing)
            }
        }
    }

    private fun handleBiometricsUnlockClick() {
        authRepository
            .getOrCreateCipher()
            ?.let { sendEvent(UnlockEvent.PromptForBiometrics(cipher = it)) }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        isBiometricsValid = false,
                        showBiometricInvalidatedMessage = !authRepository
                            .isAccountBiometricIntegrityValid(),
                        dialog = null,
                    )
                }
            }
    }

    private fun handleBiometricsUnlockSuccess(action: UnlockAction.BiometricsUnlockSuccess) {
        mutableStateFlow.update { it.copy(dialog = UnlockState.Dialog.Loading) }
        viewModelScope.launch {
            val vaultUnlockResult = authRepository.unlockWithBiometrics(cipher = action.cipher)
            sendAction(UnlockAction.Internal.ReceiveVaultUnlockResult(vaultUnlockResult))
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleBiometricsLockout() {
        mutableStateFlow.update {
            it.copy(
                dialog = UnlockState.Dialog.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.too_many_failed_biometric_attempts.asText(),
                ),
            )
        }
    }
}

/**
 * Represents state for the Unlock screen
 */
@Parcelize
data class UnlockState(
    val isBiometricsEnabled: Boolean,
    val isBiometricsValid: Boolean,
    val showBiometricInvalidatedMessage: Boolean,
    val dialog: Dialog?,
) : Parcelable {

    /**
     * Represents the various dialogs the Unlock screen can display.
     */
    @Parcelize
    sealed class Dialog : Parcelable {
        /**
         * Displays a generic error dialog to the user.
         */
        data class Error(
            val title: Text,
            val message: Text,
            val throwable: Throwable? = null,
        ) : Dialog()

        /**
         * Displays the loading dialog to the user.
         */
        data object Loading : Dialog()
    }
}

/**
 * Models events for the Unlock screen.
 */
sealed class UnlockEvent {
    /**
     * Prompts the user for biometrics unlock.
     */
    data class PromptForBiometrics(val cipher: Cipher) : UnlockEvent(), BackgroundEvent

    /**
     * Navigates to the item listing screen.
     */
    data object NavigateToItemListing : UnlockEvent()
}

/**
 * Models actions for the Unlock screen.
 */
sealed class UnlockAction {

    /**
     * The user dismissed the dialog.
     */
    data object DismissDialog : UnlockAction()

    /**
     * The user has failed biometric unlock too many times.
     */
    data object BiometricsLockout : UnlockAction()

    /**
     * The user has clicked the biometrics button.
     */
    data object BiometricsUnlockClick : UnlockAction()

    /**
     * The user has successfully unlocked the app with biometrics.
     */
    data class BiometricsUnlockSuccess(val cipher: Cipher) : UnlockAction()

    /**
     * Models actions that the [UnlockViewModel] itself might send.
     */
    sealed class Internal : UnlockAction() {
        /**
         * Indicates a vault unlock result has been received.
         */
        data class ReceiveVaultUnlockResult(
            val vaultUnlockResult: BiometricsUnlockResult,
        ) : Internal()
    }
}
