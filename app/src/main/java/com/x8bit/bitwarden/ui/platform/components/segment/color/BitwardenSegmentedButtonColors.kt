package com.x8bit.bitwarden.ui.platform.components.segment.color

import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for segmented buttons.
 */
@Composable
fun bitwardenSegmentedButtonColors(): SegmentedButtonColors = SegmentedButtonColors(
    activeContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundReversed,
    activeContentColor = BitwardenTheme.colorScheme.filledButton.foregroundReversed,
    activeBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    inactiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    inactiveContentColor = BitwardenTheme.colorScheme.text.secondary,
    inactiveBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    disabledActiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    disabledActiveContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledActiveBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    disabledInactiveContainerColor = BitwardenTheme.colorScheme.background.primary,
    disabledInactiveContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledInactiveBorderColor = BitwardenTheme.colorScheme.stroke.divider,
)
