package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.crypto.Cipher
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Models logic for the setup unlock screen.
 */
@HiltViewModel
class SetupUnlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
) : BaseViewModel<SetupUnlockState, SetupUnlockEvent, SetupUnlockAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
        val isBiometricsValid = biometricsEncryptionManager.isBiometricIntegrityValid(
            userId = userId,
            cipher = biometricsEncryptionManager.getOrCreateCipher(userId = userId),
        )
        SetupUnlockState(
            userId = userId,
            isUnlockWithPasswordEnabled = authRepository
                .userStateFlow
                .value
                ?.activeAccount
                ?.hasMasterPassword != false,
            isUnlockWithPinEnabled = settingsRepository.isUnlockWithPinEnabled,
            isUnlockWithBiometricsEnabled = settingsRepository.isUnlockWithBiometricsEnabled &&
                isBiometricsValid,
            dialogState = null,
        )
    },
) {
    override fun handleAction(action: SetupUnlockAction) {
        when (action) {
            SetupUnlockAction.ContinueClick -> handleContinueClick()
            SetupUnlockAction.EnableBiometricsClick -> handleEnableBiometricsClick()
            SetupUnlockAction.SetUpLaterClick -> handleSetUpLaterClick()
            is SetupUnlockAction.UnlockWithBiometricToggle -> {
                handleUnlockWithBiometricToggle(action)
            }

            is SetupUnlockAction.UnlockWithPinToggle -> handleUnlockWithPinToggle(action)
            is SetupUnlockAction.Internal -> handleInternalActions(action)
        }
    }

    private fun handleContinueClick() {
        sendEvent(SetupUnlockEvent.NavigateToSetupAutofill)
    }

    private fun handleEnableBiometricsClick() {
        sendEvent(
            SetupUnlockEvent.ShowBiometricsPrompt(
                // Generate a new key in case the previous one was invalidated
                cipher = biometricsEncryptionManager.createCipher(userId = state.userId),
            ),
        )
    }

    private fun handleSetUpLaterClick() {
        sendEvent(SetupUnlockEvent.NavigateToSetupAutofill)
    }

    private fun handleUnlockWithBiometricToggle(
        action: SetupUnlockAction.UnlockWithBiometricToggle,
    ) {
        if (action.isEnabled) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SetupUnlockState.DialogState.Loading(R.string.saving.asText()),
                    isUnlockWithBiometricsEnabled = true,
                )
            }
            viewModelScope.launch {
                val result = settingsRepository.setupBiometricsKey()
                sendAction(SetupUnlockAction.Internal.BiometricsKeyResultReceive(result))
            }
        } else {
            settingsRepository.clearBiometricsKey()
            mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = false) }
        }
    }

    private fun handleUnlockWithPinToggle(action: SetupUnlockAction.UnlockWithPinToggle) {
        // TODO: Handle pin unlocking logic PM-10628
    }

    private fun handleInternalActions(action: SetupUnlockAction.Internal) {
        when (action) {
            is SetupUnlockAction.Internal.BiometricsKeyResultReceive -> {
                handleBiometricsKeyResultReceive(action)
            }
        }
    }

    private fun handleBiometricsKeyResultReceive(
        action: SetupUnlockAction.Internal.BiometricsKeyResultReceive,
    ) {
        when (action.result) {
            BiometricsKeyResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        isUnlockWithBiometricsEnabled = false,
                    )
                }
            }

            BiometricsKeyResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        isUnlockWithBiometricsEnabled = true,
                    )
                }
            }
        }
    }
}

/**
 * Represents the UI state for the setup unlock screen.
 */
@Parcelize
data class SetupUnlockState(
    val userId: String,
    val isUnlockWithPasswordEnabled: Boolean,
    val isUnlockWithPinEnabled: Boolean,
    val isUnlockWithBiometricsEnabled: Boolean,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Indicates whether the continue button should be enabled or disabled.
     */
    val isContinueButtonEnabled: Boolean
        get() = isUnlockWithBiometricsEnabled || isUnlockWithPinEnabled

    /**
     * Represents the dialog UI state for the setup unlock screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Displays a loading dialog with a title.
         */
        @Parcelize
        data class Loading(
            val title: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the setup unlock screen.
 */
sealed class SetupUnlockEvent {
    /**
     * Navigate to autofill setup.
     */
    data object NavigateToSetupAutofill : SetupUnlockEvent()

    /**
     * Shows the prompt for biometrics using with the given [cipher].
     */
    data class ShowBiometricsPrompt(
        val cipher: Cipher,
    ) : SetupUnlockEvent()
}

/**
 * Models action for the setup unlock screen.
 */
sealed class SetupUnlockAction {
    /**
     * User toggled the unlock with biometrics switch.
     */
    data class UnlockWithBiometricToggle(
        val isEnabled: Boolean,
    ) : SetupUnlockAction()

    /**
     * The user clicked to enable biometrics.
     */
    data object EnableBiometricsClick : SetupUnlockAction()

    /**
     * User toggled the unlock with pin switch.
     */
    data class UnlockWithPinToggle(
        val state: UnlockWithPinState,
    ) : SetupUnlockAction()

    /**
     * The user clicked the continue button.
     */
    data object ContinueClick : SetupUnlockAction()

    /**
     * The user clicked the set up later button.
     */
    data object SetUpLaterClick : SetupUnlockAction()

    /**
     * Models actions that can be sent by the view model itself.
     */
    sealed class Internal : SetupUnlockAction() {
        /**
         * A biometrics key result has been received.
         */
        data class BiometricsKeyResultReceive(
            val result: BiometricsKeyResult,
        ) : Internal()
    }
}
