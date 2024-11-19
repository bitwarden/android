package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
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
@Suppress("TooManyFunctions")
@HiltViewModel
class SetupUnlockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<SetupUnlockState, SetupUnlockEvent, SetupUnlockAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
        val isBiometricsValid = biometricsEncryptionManager.isBiometricIntegrityValid(
            userId = userId,
            cipher = biometricsEncryptionManager.getOrCreateCipher(userId = userId),
        )
        // whether or not the user has completed the initial setup prior to this.
        val isInitialSetup = SetupUnlockArgs(savedStateHandle).isInitialSetup
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
            isInitialSetup = isInitialSetup,
        )
    },
) {
    override fun handleAction(action: SetupUnlockAction) {
        when (action) {
            SetupUnlockAction.ContinueClick -> handleContinueClick()
            SetupUnlockAction.EnableBiometricsClick -> handleEnableBiometricsClick()
            SetupUnlockAction.SetUpLaterClick -> handleSetUpLaterClick()
            SetupUnlockAction.DismissDialog -> handleDismissDialog()
            is SetupUnlockAction.UnlockWithBiometricToggle -> {
                handleUnlockWithBiometricToggle(action)
            }

            is SetupUnlockAction.UnlockWithPinToggle -> handleUnlockWithPinToggle(action)
            is SetupUnlockAction.Internal -> handleInternalActions(action)
            SetupUnlockAction.CloseClick -> handleCloseClick()
        }
    }

    private fun handleCloseClick() {
        // If the user has enabled biometric or PIN lock, but then closes the screen we
        // want to dismiss the action card.
        if (state.isContinueButtonEnabled) {
            firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        }
        sendEvent(SetupUnlockEvent.NavigateBack)
    }

    private fun handleContinueClick() {
        firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = false)
        if (state.isInitialSetup) {
            updateOnboardingStatusToNextStep()
        } else {
            sendEvent(SetupUnlockEvent.NavigateBack)
        }
    }

    private fun handleEnableBiometricsClick() {
        biometricsEncryptionManager
            .createCipherOrNull(userId = state.userId)
            ?.let {
                sendEvent(
                    SetupUnlockEvent.ShowBiometricsPrompt(
                        // Generate a new key in case the previous one was invalidated
                        cipher = it,
                    ),
                )
            }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SetupUnlockState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }
    }

    private fun handleSetUpLaterClick() {
        firstTimeActionManager.storeShowUnlockSettingBadge(showBadge = true)
        updateOnboardingStatusToNextStep()
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
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
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = action.state.isUnlockWithPinEnabled)
        }

        when (val state = action.state) {
            UnlockWithPinState.PendingEnabled -> Unit
            UnlockWithPinState.Disabled -> settingsRepository.clearUnlockPin()

            is UnlockWithPinState.Enabled -> {
                settingsRepository.storeUnlockPin(
                    pin = state.pin,
                    shouldRequireMasterPasswordOnRestart =
                    state.shouldRequireMasterPasswordOnRestart,
                )
            }
        }
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

    private fun updateOnboardingStatusToNextStep() {
        val nextStep = if (settingsRepository.isAutofillEnabledStateFlow.value) {
            OnboardingStatus.FINAL_STEP
        } else {
            OnboardingStatus.AUTOFILL_SETUP
        }
        authRepository.setOnboardingStatus(state.userId, nextStep)
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
    val isInitialSetup: Boolean,
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

        /**
         * Displays an error dialog with a title, message, and an acknowledgement button.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the setup unlock screen.
 */
sealed class SetupUnlockEvent {

    /**
     * Shows the prompt for biometrics using with the given [cipher].
     */
    data class ShowBiometricsPrompt(
        val cipher: Cipher,
    ) : SetupUnlockEvent()

    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : SetupUnlockEvent()
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
     * The user has dismissed the dialog.
     */
    data object DismissDialog : SetupUnlockAction()

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : SetupUnlockAction()

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
