package com.x8bit.bitwarden.ui.platform.components.button.color

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButtonColors
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
 * Provides a default set of Bitwarden-styled colors for an outlined button.
 */
@Composable
fun bitwardenOutlinedButtonColors(
    contentColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
    outlineColor: Color = BitwardenTheme.colorScheme.outlineButton.border,
    outlineColorDisabled: Color = BitwardenTheme.colorScheme.outlineButton.borderDisabled,
): BitwardenOutlinedButtonColors =
    BitwardenOutlinedButtonColors(
        materialButtonColors = ButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
        ),
        outlineBorderColor = outlineColor,
        outlinedDisabledBorderColor = outlineColorDisabled,
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
