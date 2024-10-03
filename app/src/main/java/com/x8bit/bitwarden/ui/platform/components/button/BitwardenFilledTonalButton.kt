package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenFilledTonalButtonColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A filled tonal button for the Bitwarden UI with a customized appearance.
 *
 * @param label The text to be displayed on the button.
 * @param onClick A lambda which will be invoked when the button is clicked.
 * @param isEnabled Whether or not the button is enabled.
 * @param modifier A [Modifier] for this composable, allowing for adjustments to its appearance
 * or behavior. This can be used to apply padding, layout, and other Modifiers.
 */
@Composable
fun BitwardenFilledTonalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        enabled = isEnabled,
        colors = bitwardenFilledTonalButtonColors(),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenFilledTonalButton_preview() {
    BitwardenTheme {
        BitwardenFilledTonalButton(
            label = "Sample Text",
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
