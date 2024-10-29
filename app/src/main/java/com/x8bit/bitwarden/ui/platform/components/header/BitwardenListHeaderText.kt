package com.x8bit.bitwarden.ui.platform.components.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled label text.
 *
 * @param label The text content for the label.
 * @param supportingLabel The optional text for the supporting label.
 * @param modifier The [Modifier] to be applied to the label.
 */
@Composable
fun BitwardenListHeaderText(
    label: String,
    supportingLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val supportLabel = supportingLabel?.let { " ($it)" }.orEmpty()
    Text(
        text = "${label.uppercase()}$supportLabel",
        style = BitwardenTheme.typography.eyebrowMedium,
        color = BitwardenTheme.colorScheme.text.secondary,
        modifier = modifier.padding(
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun BitwardenListHeaderText_preview() {
    BitwardenTheme {
        Column {
            BitwardenListHeaderText(
                label = "Sample Label",
                modifier = Modifier,
            )
            BitwardenListHeaderText(
                label = "Sample Label",
                supportingLabel = "4",
                modifier = Modifier,
            )
        }
    }
}
