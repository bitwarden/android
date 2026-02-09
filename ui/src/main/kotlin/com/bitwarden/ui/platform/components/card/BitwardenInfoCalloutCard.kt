package com.bitwarden.ui.platform.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled info callout.
 *
 * @param text The text content for the policy warning.
 * @param modifier The [Modifier] to be applied to the label.
 * @param startIcon The [IconData] to be used for the callout start icon.
 */
@Composable
fun BitwardenInfoCalloutCard(
    text: String,
    modifier: Modifier = Modifier,
    startIcon: IconData? = null,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = BitwardenTheme.shapes.infoCallout,
            )
            .padding(all = 16.dp),
    ) {
        startIcon?.let {
            BitwardenIcon(
                iconData = it,
                tint = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.size(size = 16.dp),
            )
            Spacer(modifier = Modifier.width(width = 12.dp))
        }
        Text(
            text = text,
            textAlign = TextAlign.Start,
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun BitwardenInfoCallout_preview() {
    BitwardenInfoCalloutCard(
        text = "text",
    )
}
