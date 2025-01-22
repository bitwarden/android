package com.x8bit.bitwarden.ui.platform.components.tooltip.color

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.components.tooltip.BitwardenToolTip

/**
 * Bitwarden themed colors for the [BitwardenToolTip]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun bitwardenTooltipColors(
    contentColor: Color = BitwardenTheme.colorScheme.text.primary,
    containerColor: Color = BitwardenTheme.colorScheme.background.secondary,
    titleContentColor: Color = BitwardenTheme.colorScheme.text.secondary,
    actionContentColor: Color = BitwardenTheme.colorScheme.text.interaction,
): RichTooltipColors = RichTooltipColors(
    contentColor = contentColor,
    containerColor = containerColor,
    titleContentColor = titleContentColor,
    actionContentColor = actionContentColor,
)
