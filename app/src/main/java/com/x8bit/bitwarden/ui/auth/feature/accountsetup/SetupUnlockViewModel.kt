package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.BiometricsEncryptionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
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
            isUnlockWithPasswordEnabled = authRepository
                .userStateFlow
                .value
                ?.activeAccount
                ?.hasMasterPassword != false,
            isUnlockWithPinEnabled = settingsRepository.isUnlockWithPinEnabled,
            isUnlockWithBiometricsEnabled = settingsRepository.isUnlockWithBiometricsEnabled &&
                isBiometricsValid,
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
        }
    }

    private fun handleContinueClick() {
        sendEvent(SetupUnlockEvent.NavigateToSetupAutofill)
    }

    private fun handleEnableBiometricsClick() {
        // TODO: Handle biometric unlocking logic PM-10624
    }

    private fun handleSetUpLaterClick() {
        sendEvent(SetupUnlockEvent.NavigateToSetupAutofill)
    }

    private fun handleUnlockWithBiometricToggle(
        action: SetupUnlockAction.UnlockWithBiometricToggle,
    ) {
        // TODO: Handle biometric unlocking logic PM-10624
    }

    private fun handleUnlockWithPinToggle(action: SetupUnlockAction.UnlockWithPinToggle) {
        // TODO: Handle pin unlocking logic PM-10628
    }
}

/**
 * Represents the UI state for the setup unlock screen.
 */
@Parcelize
data class SetupUnlockState(
    val isUnlockWithPasswordEnabled: Boolean,
    val isUnlockWithPinEnabled: Boolean,
    val isUnlockWithBiometricsEnabled: Boolean,
) : Parcelable {
    /**
     * Indicates whether the continue button should be enabled or disabled.
     */
    val isContinueButtonEnabled: Boolean
        get() = isUnlockWithBiometricsEnabled || isUnlockWithPinEnabled
}

/**
 * Models events for the setup unlock screen.
 */
sealed class SetupUnlockEvent {
    /**
     * Navigate to autofill setup.
     */
    data object NavigateToSetupAutofill : SetupUnlockEvent()
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
}
