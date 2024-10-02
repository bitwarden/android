package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * @param labelTextColor The color for the label text.
 */
@Composable
fun BitwardenTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    labelTextColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = bitwardenTextButtonColors(contentColor = labelTextColor),
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenTextButton_preview() {
    BitwardenTextButton(
        label = "Label",
        onClick = {},
    )
}
