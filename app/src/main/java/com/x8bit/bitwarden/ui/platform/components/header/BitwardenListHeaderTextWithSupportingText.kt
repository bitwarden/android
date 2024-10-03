package com.x8bit.bitwarden.ui.platform.components.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled label text.
 *
 * @param label The text content for the label.
 * @param supportingLabel The text for the supporting label.
 * @param modifier The [Modifier] to be applied to the label.
 */
@Composable
fun BitwardenListHeaderTextWithSupportLabel(
    label: String,
    supportingLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                top = 12.dp,
                bottom = 4.dp,
                end = 8.dp,
            )
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
        )

        Text(
            text = supportingLabel,
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.secondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenListHeaderTextWithSupportLabel_preview() {
    BitwardenTheme {
        BitwardenListHeaderTextWithSupportLabel(
            label = "Sample Label",
            supportingLabel = "0",
            modifier = Modifier,
        )
    }
}
