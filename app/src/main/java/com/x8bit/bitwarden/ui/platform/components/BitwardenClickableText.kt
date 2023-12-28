package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * Represents a Bitwarden-styled clickable text.
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 */
@Composable
fun BitwardenClickableText(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            // TODO: Need to figure out better handling for very small clickable text (BIT-724)
            .clickable { onClick() },
        text = label,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview
@Composable
private fun BitwardenTextButton_preview() {
    BitwardenTextButton(
        label = "Label",
        onClick = {},
    )
}
