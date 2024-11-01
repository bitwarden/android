package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricSupportStatus

/**
 * Displays a switch for enabling or disabling unlock with biometrics functionality.
 *
 * @param isChecked Indicates that the switch should be checked or not.
 * @param biometricSupportStatus Indicates what type of biometrics are supported on device.
 * @param onDisableBiometrics Callback invoked when the toggle has be turned off.
 * @param onEnableBiometrics Callback invoked when the toggle has be turned on.
 * @param modifier The [Modifier] to be applied to the switch.
 */
@Composable
fun BitwardenUnlockWithBiometricsSwitch(
    biometricSupportStatus: BiometricSupportStatus,
    isChecked: Boolean,
    onDisableBiometrics: () -> Unit,
    onEnableBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val biometricsDescription: String = when (biometricSupportStatus) {
        BiometricSupportStatus.CLASS_3_SUPPORTED -> {
            stringResource(R.string.class_3_biometrics_description)
        }

        BiometricSupportStatus.CLASS_2_SUPPORTED -> {
            stringResource(R.string.class_2_biometrics_description)
        }

        BiometricSupportStatus.NOT_SUPPORTED -> return
    }
    BitwardenSwitch(
        modifier = modifier,
        label = stringResource(
            id = R.string.unlock_with,
            stringResource(id = R.string.biometrics),
        ),
        isChecked = isChecked,
        onCheckedChange = { toggled ->
            if (toggled) {
                onEnableBiometrics()
            } else {
                onDisableBiometrics()
            }
        },
        enabled = biometricSupportStatus == BiometricSupportStatus.CLASS_3_SUPPORTED,
        description = biometricsDescription,
    )
}
