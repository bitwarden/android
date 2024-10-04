package com.x8bit.bitwarden.ui.platform.components.appbar.color

import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for menu items.
 */
@Composable
fun bitwardenMenuItemColors(): MenuItemColors = MenuItemColors(
    textColor = BitwardenTheme.colorScheme.text.primary,
    leadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    trailingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledTextColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledLeadingIconColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
    disabledTrailingIconColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)
