package com.x8bit.bitwarden.ui.platform.components.button.color

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for a filled button.
 */
@Composable
fun bitwardenFilledButtonColors(): ButtonColors = ButtonColors(
    containerColor = BitwardenTheme.colorScheme.filledButton.background,
    contentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    disabledContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for a filled error button.
 */
@Composable
fun bitwardenFilledErrorButtonColors() = ButtonColors(
    containerColor = BitwardenTheme.colorScheme.status.weak1,
    contentColor = BitwardenTheme.colorScheme.filledButton.foreground,
    disabledContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for a tonal button.
 */
@Composable
fun bitwardenFilledTonalButtonColors(): ButtonColors = ButtonColors(
    containerColor = BitwardenTheme.colorScheme.tonalButton.background,
    contentColor = BitwardenTheme.colorScheme.tonalButton.foreground,
    disabledContainerColor = BitwardenTheme.colorScheme.tonalButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.tonalButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for an outlined button.
 */
@Composable
fun bitwardenOutlinedButtonColors(
    contentColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
): ButtonColors =
    ButtonColors(
        containerColor = Color.Transparent,
        contentColor = contentColor,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    )

/**
 * Provides a default set of Bitwarden-styled colors for an outlined error button.
 */
@Composable
fun bitwardenOutlinedErrorButtonColors(): ButtonColors = ButtonColors(
    containerColor = Color.Transparent,
    contentColor = BitwardenTheme.colorScheme.status.error,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
)

/**
 * Provides a default set of Bitwarden-styled colors for a text button.
 */
@Composable
fun bitwardenTextButtonColors(
    contentColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
): ButtonColors = ButtonColors(
    containerColor = Color.Transparent,
    contentColor = contentColor,
    disabledContainerColor = Color.Transparent,
    disabledContentColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
)
