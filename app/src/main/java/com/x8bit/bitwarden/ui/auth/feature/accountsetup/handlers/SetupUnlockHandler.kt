package com.x8bit.bitwarden.ui.auth.feature.accountsetup.handlers

import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupUnlockAction
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.SetupUnlockViewModel
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState

/**
 * A collection of handler functions for managing actions within the context of the Setup Unlock
 * Screen.
 */
data class SetupUnlockHandler(
    val onDisableBiometrics: () -> Unit,
    val onEnableBiometrics: () -> Unit,
    val onUnlockWithPinToggle: (UnlockWithPinState) -> Unit,
    val onContinueClick: () -> Unit,
    val onSetUpLaterClick: () -> Unit,
    val unlockWithBiometricToggle: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [SetupUnlockHandler] by binding actions to the provided
         * [SetupUnlockViewModel].
         */
        fun create(viewModel: SetupUnlockViewModel): SetupUnlockHandler =
            SetupUnlockHandler(
                onDisableBiometrics = {
                    viewModel.trySendAction(
                        SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = false),
                    )
                },
                onEnableBiometrics = {
                    viewModel.trySendAction(SetupUnlockAction.EnableBiometricsClick)
                },
                onUnlockWithPinToggle = {
                    viewModel.trySendAction(SetupUnlockAction.UnlockWithPinToggle(it))
                },
                onContinueClick = { viewModel.trySendAction(SetupUnlockAction.ContinueClick) },
                onSetUpLaterClick = { viewModel.trySendAction(SetupUnlockAction.SetUpLaterClick) },
                unlockWithBiometricToggle = {
                    viewModel.trySendAction(
                        SetupUnlockAction.UnlockWithBiometricToggle(isEnabled = true),
                    )
                },
            )
    }
}
