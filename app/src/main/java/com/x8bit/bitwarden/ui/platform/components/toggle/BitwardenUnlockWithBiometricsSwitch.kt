package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R

/**
 * Displays a switch for enabling or disabling unlock with biometrics functionality.
 *
 * @param isChecked Indicates that the switch should be checked or not.
 * @param isBiometricsSupported Indicates if biometrics is supported and we should display the
 * switch.
 * @param onDisableBiometrics Callback invoked when the toggle has be turned off.
 * @param onEnableBiometrics Callback invoked when the toggle has be turned on.
 * @param modifier The [Modifier] to be applied to the switch.
 */
@Composable
fun BitwardenUnlockWithBiometricsSwitch(
    isBiometricsSupported: Boolean,
    isChecked: Boolean,
    onDisableBiometrics: () -> Unit,
    onEnableBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isBiometricsSupported) return
    BitwardenWideSwitch(
        modifier = modifier,
        label = stringResource(id = R.string.unlock_with, stringResource(id = R.string.biometrics)),
        isChecked = isChecked,
        onCheckedChange = { toggled ->
            if (toggled) {
                onEnableBiometrics()
            } else {
                onDisableBiometrics()
            }
        },
    )
}
