package com.bitwarden.ui.platform.base.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Creates a new [SpanStyle] from the specified [color] and [textStyle].
 */
fun spanStyleOf(
    color: Color,
    textStyle: TextStyle,
): SpanStyle =
    SpanStyle(
        color = color,
        fontSize = textStyle.fontSize,
        fontWeight = textStyle.fontWeight,
        fontStyle = textStyle.fontStyle,
        fontSynthesis = textStyle.fontSynthesis,
        fontFamily = textStyle.fontFamily,
        fontFeatureSettings = textStyle.fontFeatureSettings,
        letterSpacing = textStyle.letterSpacing,
        baselineShift = textStyle.baselineShift,
        textGeometricTransform = textStyle.textGeometricTransform,
        localeList = textStyle.localeList,
        background = textStyle.background,
        textDecoration = textStyle.textDecoration,
        shadow = textStyle.shadow,
        platformStyle = textStyle.platformStyle?.spanStyle,
        drawStyle = textStyle.drawStyle,
    )

/**
 * A [SpanStyle] representing "regular" text using the text primary color and label medium font.
 */
val bitwardenDefaultSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = spanStyleOf(
        color = BitwardenTheme.colorScheme.text.primary,
        textStyle = BitwardenTheme.typography.labelMedium,
    )

/**
 * A [SpanStyle] representing "bold" text using the text primary color and body medium emphasis
 * font.
 */
val bitwardenBoldSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = spanStyleOf(
        color = BitwardenTheme.colorScheme.text.primary,
        textStyle = BitwardenTheme.typography.bodyMediumEmphasis,
    )

/**
 * A [SpanStyle] representing "clickable" text using the text interaction color and label medium
 * font.
 */
val bitwardenClickableTextSpanStyle: SpanStyle
    @Composable
    @ReadOnlyComposable
    get() = spanStyleOf(
        color = BitwardenTheme.colorScheme.text.interaction,
        textStyle = BitwardenTheme.typography.labelMedium,
    )
