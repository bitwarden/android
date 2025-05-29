package com.bitwarden.ui.platform.components.appbar.color

import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for menu items.
 */
@Composable
fun bitwardenMenuItemColors(
    textColor: Color = BitwardenTheme.colorScheme.text.primary,
): MenuItemColors = MenuItemColors(
    textColor = textColor,
    leadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    trailingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledTextColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledLeadingIconColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledTrailingIconColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)
