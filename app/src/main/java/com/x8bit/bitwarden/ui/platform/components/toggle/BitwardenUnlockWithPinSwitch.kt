package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.PinInputDialog

/**
 * Displays a switch for enabling or disabling unlock with pin functionality.
 *
 * @param isUnlockWithPasswordEnabled Indicates whether or not the password unlocking is enabled.
 * @param isUnlockWithPinEnabled Indicates whether or not the pin unlocking is enabled.
 * @param onUnlockWithPinToggleAction Callback that is invoked when the current state of the switch
 * changes.
 * @param modifier The [Modifier] to be applied to the switch.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenUnlockWithPinSwitch(
    isUnlockWithPasswordEnabled: Boolean,
    isUnlockWithPinEnabled: Boolean,
    onUnlockWithPinToggleAction: (UnlockWithPinState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowPinInputDialog by rememberSaveable { mutableStateOf(value = false) }
    var shouldShowPinConfirmationDialog by rememberSaveable { mutableStateOf(value = false) }
    var pin by remember { mutableStateOf(value = "") }
    BitwardenSwitch(
        label = stringResource(id = R.string.unlock_with_pin),
        isChecked = isUnlockWithPinEnabled,
        onCheckedChange = { isChecked ->
            if (isChecked) {
                onUnlockWithPinToggleAction(UnlockWithPinState.PendingEnabled)
                shouldShowPinInputDialog = true
            } else {
                onUnlockWithPinToggleAction(UnlockWithPinState.Disabled)
            }
        },
        modifier = modifier,
    )

    when {
        shouldShowPinInputDialog -> {
            PinInputDialog(
                onCancelClick = {
                    shouldShowPinInputDialog = false
                    onUnlockWithPinToggleAction(UnlockWithPinState.Disabled)
                    pin = ""
                },
                onSubmitClick = {
                    pin = it
                    if (pin.isNotEmpty()) {
                        shouldShowPinInputDialog = false
                        if (isUnlockWithPasswordEnabled) {
                            shouldShowPinConfirmationDialog = true
                            onUnlockWithPinToggleAction(UnlockWithPinState.PendingEnabled)
                        } else {
                            onUnlockWithPinToggleAction(
                                UnlockWithPinState.Enabled(
                                    pin = pin,
                                    shouldRequireMasterPasswordOnRestart = false,
                                ),
                            )
                        }
                    } else {
                        shouldShowPinInputDialog = false
                        onUnlockWithPinToggleAction(UnlockWithPinState.Disabled)
                    }
                },
                onDismissRequest = {
                    shouldShowPinInputDialog = false
                    onUnlockWithPinToggleAction(UnlockWithPinState.Disabled)
                    pin = ""
                },
                isPinCreation = true,
            )
        }

        shouldShowPinConfirmationDialog -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.require_master_password_on_app_restart),
                message = stringResource(id = R.string.pin_require_master_password_restart),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.no),
                onConfirmClick = {
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        UnlockWithPinState.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = true,
                        ),
                    )
                    pin = ""
                },
                onDismissClick = {
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        UnlockWithPinState.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = false,
                        ),
                    )
                    pin = ""
                },
                onDismissRequest = {
                    // Dismissing the dialog is the same as requiring a master password
                    // confirmation.
                    shouldShowPinConfirmationDialog = false
                    onUnlockWithPinToggleAction(
                        UnlockWithPinState.Enabled(
                            pin = pin,
                            shouldRequireMasterPasswordOnRestart = true,
                        ),
                    )
                    pin = ""
                },
            )
        }
    }
}

/**
 * User toggled the unlock with pin switch.
 */
sealed class UnlockWithPinState {
    /**
     * Whether or not the action represents PIN unlocking being enabled.
     */
    abstract val isUnlockWithPinEnabled: Boolean

    /**
     * The toggle was disabled.
     */
    data object Disabled : UnlockWithPinState() {
        override val isUnlockWithPinEnabled: Boolean get() = false
    }

    /**
     * The toggle was enabled but the behavior is pending confirmation.
     */
    data object PendingEnabled : UnlockWithPinState() {
        override val isUnlockWithPinEnabled: Boolean get() = true
    }

    /**
     * The toggle was enabled and the user's [pin] and [shouldRequireMasterPasswordOnRestart]
     * preference were confirmed.
     */
    data class Enabled(
        val pin: String,
        val shouldRequireMasterPasswordOnRestart: Boolean,
    ) : UnlockWithPinState() {
        override val isUnlockWithPinEnabled: Boolean get() = true
    }
}
