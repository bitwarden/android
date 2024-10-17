package com.x8bit.bitwarden.ui.platform.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * An overloaded version [BitwardenContentBlock] which takes a [String] for the header text.
 */
@Composable
fun BitwardenContentBlock(
    headerText: String,
    modifier: Modifier = Modifier,
    headerTextStyle: TextStyle = BitwardenTheme.typography.titleSmall,
    subtitleText: String? = null,
    subtitleTextStyle: TextStyle = BitwardenTheme.typography.bodyMedium,
    iconPainter: Painter? = null,
    backgroundColor: Color = BitwardenTheme.colorScheme.background.secondary,
) {
    BitwardenContentBlock(
        headerText = AnnotatedString(headerText),
        modifier = modifier,
        headerTextStyle = headerTextStyle,
        subtitleText = subtitleText,
        subtitleTextStyle = subtitleTextStyle,
        iconPainter = iconPainter,
        backgroundColor = backgroundColor,
    )
}

/**
 * A default content block which displays a header with an optional subtitle and an icon.
 * Implemented to match design component.
 */
@Composable
fun BitwardenContentBlock(
    headerText: AnnotatedString,
    modifier: Modifier = Modifier,
    headerTextStyle: TextStyle = BitwardenTheme.typography.titleSmall,
    subtitleText: String? = null,
    subtitleTextStyle: TextStyle = BitwardenTheme.typography.bodyMedium,
    iconPainter: Painter? = null,
    backgroundColor: Color = BitwardenTheme.colorScheme.background.secondary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconPainter
            ?.let {
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = it,
                    contentDescription = null,
                    tint = BitwardenTheme.colorScheme.icon.secondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            ?: Spacer(Modifier.width(16.dp))

        Column {
            Spacer(Modifier.height(12.dp))
            Text(
                text = headerText,
                style = headerTextStyle,
            )
            subtitleText?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = subtitleTextStyle,
                    color = BitwardenTheme.colorScheme.text.secondary,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.width(16.dp))
    }
}

@Preview
@Composable
private fun BitwardenContentBlock_preview() {
    BitwardenTheme {
        BitwardenContentBlock(
            headerText = "Header",
            subtitleText = "Subtitle",
            iconPainter = null,
        )
    }
}
