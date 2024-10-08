package com.x8bit.bitwarden.ui.platform.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled info callout.
 *
 * @param text The text content for the policy warning.
 * @param modifier The [Modifier] to be applied to the label.
 */
@Composable
fun BitwardenInfoCalloutCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        textAlign = TextAlign.Start,
        style = BitwardenTheme.typography.bodyMedium,
        color = BitwardenTheme.colorScheme.text.primary,
        modifier = modifier
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = BitwardenTheme.shapes.infoCallout,
            )
            .padding(all = 16.dp),
    )
}

@Preview
@Composable
private fun BitwardenInfoCallout_preview() {
    BitwardenInfoCalloutCard(
        text = "text",
    )
}
