package com.x8bit.bitwarden.ui.platform.theme.type

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

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
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    val sensitiveInfoSmall: TextStyle,
    val sensitiveInfoMedium: TextStyle,
    val eyebrowMedium: TextStyle,
)
