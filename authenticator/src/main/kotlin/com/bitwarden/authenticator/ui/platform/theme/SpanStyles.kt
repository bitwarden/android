package com.bitwarden.authenticator.ui.platform.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

/**
 * Defines a span style for clickable span texts. Useful because spans require a
 * [SpanStyle] instead of the typical [TextStyle].
 */
@Composable
@ReadOnlyComposable
fun clickableSpanStyle(): SpanStyle = SpanStyle(
    color = MaterialTheme.colorScheme.primary,
    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
    fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
)
