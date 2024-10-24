package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenTextButtonColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled [TextButton].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param labelTextColor The color for the label text.
 */
@Composable
fun BitwardenTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
    labelTextColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
) {
    TextButton(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick,
        enabled = isEnabled,
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = 12.dp,
            end = if (icon == null) 12.dp else 16.dp,
        ),
        colors = bitwardenTextButtonColors(contentColor = labelTextColor),
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenTextButton_preview() {
    Column {
        BitwardenTextButton(
            label = "Label",
            onClick = {},
            isEnabled = true,
        )
        BitwardenTextButton(
            label = "Label",
            onClick = {},
            isEnabled = false,
        )
    }
}
