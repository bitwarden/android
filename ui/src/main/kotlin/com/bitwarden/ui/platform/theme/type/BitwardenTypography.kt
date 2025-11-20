package com.bitwarden.ui.platform.theme.type

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Defines all the text-styles for the app.
 */
@Immutable
data class BitwardenTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodyMediumEmphasis: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val sensitiveInfoSmall: TextStyle,
    val sensitiveInfoMedium: TextStyle,
    val eyebrowMedium: TextStyle,
)

/**
 * Updates the textAlign property of all text-styles.
 * @param newTextAlign The new text alignment to be used.
 */
fun BitwardenTypography.updateTextAlign(newTextAlign: TextAlign): BitwardenTypography {
    return this.copy(
        displayLarge = displayLarge.copy(textAlign = newTextAlign),
        displayMedium = displayMedium.copy(textAlign = newTextAlign),
        displaySmall = displaySmall.copy(textAlign = newTextAlign),
        headlineLarge = headlineLarge.copy(textAlign = newTextAlign),
        headlineMedium = headlineMedium.copy(textAlign = newTextAlign),
        headlineSmall = headlineSmall.copy(textAlign = newTextAlign),
        titleLarge = titleLarge.copy(textAlign = newTextAlign),
        titleMedium = titleMedium.copy(textAlign = newTextAlign),
        titleSmall = titleSmall.copy(textAlign = newTextAlign),
        bodyLarge = bodyLarge.copy(textAlign = newTextAlign),
        bodyMedium = bodyMedium.copy(textAlign = newTextAlign),
        bodyMediumEmphasis = bodyMediumEmphasis.copy(textAlign = newTextAlign),
        bodySmall = bodySmall.copy(textAlign = newTextAlign),
        labelLarge = labelLarge.copy(textAlign = newTextAlign),
        labelMedium = labelMedium.copy(textAlign = newTextAlign),
        labelSmall = labelSmall.copy(textAlign = newTextAlign),
        sensitiveInfoSmall = sensitiveInfoSmall.copy(textAlign = newTextAlign),
        sensitiveInfoMedium = sensitiveInfoMedium.copy(textAlign = newTextAlign),
        eyebrowMedium = eyebrowMedium.copy(textAlign = newTextAlign),
    )
}
