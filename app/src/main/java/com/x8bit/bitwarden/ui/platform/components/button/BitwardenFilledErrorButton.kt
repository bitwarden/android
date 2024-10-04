package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenFilledErrorButtonColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled filled [Button] for error scenarios.
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param isEnabled Whether or not the button is enabled.
 */
@Composable
fun BitwardenFilledErrorButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = isEnabled,
        contentPadding = PaddingValues(
            vertical = 10.dp,
            horizontal = 24.dp,
        ),
        colors = bitwardenFilledErrorButtonColors(),
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun BitwardenErrorButton_preview_isEnabled() {
    BitwardenTheme {
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            isEnabled = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenErrorButton_preview_isNotEnabled() {
    BitwardenTheme {
        BitwardenFilledErrorButton(
            label = "Label",
            onClick = {},
            isEnabled = false,
        )
    }
}
