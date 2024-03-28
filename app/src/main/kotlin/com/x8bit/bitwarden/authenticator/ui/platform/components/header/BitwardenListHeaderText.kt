package com.x8bit.bitwarden.authenticator.ui.platform.components.header

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * Represents a Bitwarden-styled label text.
 *
 * @param label The text content for the label.
 * @param modifier The [Modifier] to be applied to the label.
 */
@Composable
fun BitwardenListHeaderText(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun BitwardenListHeaderText_preview() {
    AuthenticatorTheme {
        BitwardenListHeaderText(
            label = "Sample Label",
            modifier = Modifier,
        )
    }
}
