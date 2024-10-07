package com.x8bit.bitwarden.ui.platform.components.button

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenStandardIconButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A standard icon button that displays an icon.
 *
 * @param vectorIconRes Icon to display on the button.
 * @param contentDescription The content description for this icon button.
 * @param onClick Callback for when the icon button is clicked.
 * @param modifier A [Modifier] for the composable.
 * @param isEnabled Whether or not the button should be enabled.
 */
@Composable
fun BitwardenStandardIconButton(
    @DrawableRes vectorIconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentColor: Color = BitwardenTheme.colorScheme.icon.primary,
) {
    BitwardenStandardIconButton(
        painter = rememberVectorPainter(id = vectorIconRes),
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        isEnabled = isEnabled,
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
 * @param isEnabled Whether or not the button should be enabled.
 */
@Composable
fun BitwardenStandardIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    contentColor: Color = BitwardenTheme.colorScheme.icon.primary,
) {
    IconButton(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick,
        colors = bitwardenStandardIconButtonColors(contentColor = contentColor),
        enabled = isEnabled,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenStandardIconButton_preview() {
    BitwardenTheme {
        BitwardenStandardIconButton(
            vectorIconRes = R.drawable.ic_question_circle,
            contentDescription = "Sample Icon",
            onClick = {},
        )
    }
}
