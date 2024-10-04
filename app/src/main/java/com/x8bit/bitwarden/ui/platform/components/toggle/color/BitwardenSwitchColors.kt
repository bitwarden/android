package com.x8bit.bitwarden.ui.platform.components.toggle.color

import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for switches.
 */
@Composable
fun bitwardenSwitchColors(): SwitchColors = SwitchColors(
    checkedThumbColor = BitwardenTheme.colorScheme.toggleButton.switch,
    checkedTrackColor = BitwardenTheme.colorScheme.toggleButton.backgroundOn,
    checkedBorderColor = Color.Transparent,
    checkedIconColor = BitwardenTheme.colorScheme.toggleButton.backgroundOn,
    uncheckedThumbColor = BitwardenTheme.colorScheme.toggleButton.switch,
    uncheckedTrackColor = BitwardenTheme.colorScheme.toggleButton.backgroundOff,
    uncheckedBorderColor = Color.Transparent,
    uncheckedIconColor = BitwardenTheme.colorScheme.toggleButton.backgroundOn,
    disabledCheckedThumbColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledCheckedTrackColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledCheckedBorderColor = Color.Transparent,
    disabledCheckedIconColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledUncheckedThumbColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledUncheckedTrackColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledUncheckedBorderColor = Color.Transparent,
    disabledUncheckedIconColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
)
