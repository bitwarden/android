package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenOutlinedButtonColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled [OutlinedButton].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    colors: ButtonColors = bitwardenOutlinedButtonColors(),
    outlineBorderColor: Color = BitwardenTheme.colorScheme.outlineButton.border,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .semantics(mergeDescendants = true) { },
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = colors,
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) {
                outlineBorderColor
            } else {
                BitwardenTheme.colorScheme.outlineButton.borderDisabled
            },
        ),
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenOutlinedButton_preview_isEnabled() {
    BitwardenOutlinedButton(
        label = "Label",
        onClick = {},
        isEnabled = true,
    )
}

@Preview
@Composable
private fun BitwardenOutlinedButton_preview_isNotEnabled() {
    BitwardenOutlinedButton(
        label = "Label",
        onClick = {},
        isEnabled = false,
    )
}
