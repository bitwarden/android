package com.x8bit.bitwarden.authenticator.ui.platform.components.button

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
    labelTextColor: Color? = null,
) {
    val defaultColors = if (labelTextColor != null) {
        ButtonDefaults.textButtonColors(
            contentColor = labelTextColor,
        )
    } else {
        ButtonDefaults.textButtonColors()
    }

    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        colors = defaultColors,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(
                    vertical = 10.dp,
                    horizontal = 12.dp,
                ),
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
