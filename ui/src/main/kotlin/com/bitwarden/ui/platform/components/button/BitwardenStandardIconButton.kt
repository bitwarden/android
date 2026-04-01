package com.bitwarden.ui.platform.components.button

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.button.color.bitwardenStandardIconButtonColors
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A standard icon button that displays an icon.
 *
 * @param vectorIconRes Icon to display on the button.
 * @param contentDescription The content description for this icon button.
 * @param onClick Callback for when the icon button is clicked.
 * @param modifier A [Modifier] for the composable.
 * @param isEnabled Whether the button should be enabled.
 * @param isExternalLink Whether the icon button is an external link.
 * @param contentColor The color applied to the icon.
 */
@Composable
fun BitwardenStandardIconButton(
    @DrawableRes vectorIconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isExternalLink: Boolean = false,
    contentColor: Color = BitwardenTheme.colorScheme.icon.primary,
) {
    BitwardenStandardIconButton(
        painter = rememberVectorPainter(id = vectorIconRes),
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        isEnabled = isEnabled,
        isExternalLink = isExternalLink,
        contentColor = contentColor,
    )
}

/**
 * A standard icon button that displays an icon.
 *
 * @param painter Painter icon to display on the button.
 * @param contentDescription The content description for this icon button.
 * @param onClick Callback for when the icon button is clicked.
 * @param modifier A [Modifier] for the composable.
 * @param isEnabled Whether the button should be enabled.
 * @param isExternalLink Whether the icon button is an external link.
 * @param contentColor The color applied to the icon.
 */
@Composable
fun BitwardenStandardIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isExternalLink: Boolean = false,
    contentColor: Color = BitwardenTheme.colorScheme.icon.primary,
) {
    val formattedContentDescription = if (isExternalLink) {
        stringResource(
            id = BitwardenString.external_link_format,
            formatArgs = arrayOf(contentDescription),
        )
    } else {
        contentDescription
    }
    IconButton(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = formattedContentDescription
        },
        onClick = onClick,
        colors = bitwardenStandardIconButtonColors(contentColor = contentColor),
        enabled = isEnabled,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenStandardIconButton_preview() {
    BitwardenTheme {
        BitwardenStandardIconButton(
            vectorIconRes = BitwardenDrawable.ic_question_circle,
            contentDescription = "Sample Icon",
            onClick = {},
        )
    }
}
