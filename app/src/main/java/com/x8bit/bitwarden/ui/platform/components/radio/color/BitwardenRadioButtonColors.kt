package com.x8bit.bitwarden.ui.platform.components.radio.color

import androidx.compose.material3.RadioButtonColors
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for radio buttons.
 */
@Composable
fun bitwardenRadioButtonColors(): RadioButtonColors = RadioButtonColors(
    selectedColor = BitwardenTheme.colorScheme.filledButton.background,
    unselectedColor = BitwardenTheme.colorScheme.icon.primary,
    disabledSelectedColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledUnselectedColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)
