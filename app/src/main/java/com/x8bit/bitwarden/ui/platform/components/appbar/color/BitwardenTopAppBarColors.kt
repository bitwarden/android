package com.x8bit.bitwarden.ui.platform.components.appbar.color

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for top app bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun bitwardenTopAppBarColors(): TopAppBarColors = TopAppBarColors(
    containerColor = BitwardenTheme.colorScheme.background.secondary,
    scrolledContainerColor = BitwardenTheme.colorScheme.background.secondary,
    navigationIconContentColor = BitwardenTheme.colorScheme.icon.primary,
    titleContentColor = BitwardenTheme.colorScheme.text.primary,
    actionIconContentColor = BitwardenTheme.colorScheme.icon.primary,
)
