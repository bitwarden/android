package com.x8bit.bitwarden.ui.platform.components.segment.color

import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for segmented buttons.
 */
@Composable
fun bitwardenSegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonColors(
    activeContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundReversed,
    activeContentColor = BitwardenTheme.colorScheme.filledButton.foregroundReversed,
    activeBorderColor = Color.Transparent,
    inactiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    inactiveContentColor = BitwardenTheme.colorScheme.text.secondary,
    inactiveBorderColor = Color.Transparent,
    disabledActiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    disabledActiveContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledActiveBorderColor = Color.Transparent,
    disabledInactiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    disabledInactiveContentColor = BitwardenTheme.colorScheme.stroke.divider,
    disabledInactiveBorderColor = Color.Transparent,
)
