package com.bitwarden.ui.platform.components.appbar.color

import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for top app bars.
 */
@Composable
fun bitwardenTopAppBarColors(): TopAppBarColors = TopAppBarColors(
    containerColor = BitwardenTheme.colorScheme.background.secondary,
    scrolledContainerColor = BitwardenTheme.colorScheme.background.secondary,
    navigationIconContentColor = BitwardenTheme.colorScheme.icon.primary,
    titleContentColor = BitwardenTheme.colorScheme.text.primary,
    actionIconContentColor = BitwardenTheme.colorScheme.icon.primary,
    subtitleContentColor = BitwardenTheme.colorScheme.text.primary,
)
