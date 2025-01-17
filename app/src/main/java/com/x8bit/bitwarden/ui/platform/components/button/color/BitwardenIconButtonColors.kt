package com.x8bit.bitwarden.ui.platform.components.button.color

import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for a filled icon button.
 */
@Composable
fun bitwardenFilledIconButtonColors(): IconButtonColors = IconButtonColors(
    containerColor = BitwardenTheme.colorScheme.filledButton.background,
    contentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    disabledContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for a standard icon button.
 */
@Composable
fun bitwardenStandardIconButtonColors(
    contentColor: Color = BitwardenTheme.colorScheme.icon.primary,
): IconButtonColors = IconButtonColors(
    containerColor = Color.Transparent,
    contentColor = contentColor,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for a filled icon button.
 */
@Composable
fun bitwardenTonalIconButtonColors(): IconButtonColors = IconButtonColors(
    containerColor = BitwardenTheme.colorScheme.background.tertiary,
    contentColor = BitwardenTheme.colorScheme.filledButton.foregroundReversed,
    disabledContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)
