package com.bitwarden.ui.platform.components.card.color

import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for a card.
 */
@Composable
fun bitwardenCardColors(
    containerColor: Color = BitwardenTheme.colorScheme.background.tertiary,
    contentColor: Color = BitwardenTheme.colorScheme.text.primary,
    disabledContainerColor: Color = BitwardenTheme.colorScheme.filledButton.backgroundDisabled,
    disabledContentColor: Color = BitwardenTheme.colorScheme.filledButton.foregroundDisabled,
): CardColors {
    return CardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )
}
