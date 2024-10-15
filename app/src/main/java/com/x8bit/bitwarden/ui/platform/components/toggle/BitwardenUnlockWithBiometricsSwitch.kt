package com.x8bit.bitwarden.ui.platform.components.toggle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricSupportStatus
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
    biometricSupportStatus: BiometricSupportStatus,
    isChecked: Boolean,
    onDisableBiometrics: () -> Unit,
    onEnableBiometrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (biometricSupportStatus == BiometricSupportStatus.NOT_SUPPORTED) return
    val biometricsDescription: String = when (biometricSupportStatus) {
        BiometricSupportStatus.CLASS_3_SUPPORTED -> {
            stringResource(R.string.class_3_biometrics_description)
        }
        BiometricSupportStatus.CLASS_2_SUPPORTED -> {
            stringResource(R.string.class_2_biometrics_description)
        }

        BiometricSupportStatus.NOT_SUPPORTED -> error(
            "Should not be called when BiometricSupportStatus is NOT_SUPPORTED",
        )
    }
    Column(modifier = modifier) {
        BitwardenWideSwitch(
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
}
