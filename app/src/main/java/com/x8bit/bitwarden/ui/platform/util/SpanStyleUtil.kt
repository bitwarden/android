package com.x8bit.bitwarden.ui.platform.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

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
