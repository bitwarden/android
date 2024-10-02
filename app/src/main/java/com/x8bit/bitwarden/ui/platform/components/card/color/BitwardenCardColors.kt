package com.x8bit.bitwarden.ui.platform.components.card.color

import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for a card.
 */
@Composable
fun bitwardenCardColors(): CardColors = CardColors(
    containerColor = BitwardenTheme.colorScheme.background.tertiary,
    contentColor = BitwardenTheme.colorScheme.text.primary,
    disabledContainerColor = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
)
