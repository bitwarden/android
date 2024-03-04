package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Represents a Bitwarden-styled policy warning label.
 *
 * @param text The text content for the policy warning.
 * @param textAlign The text alignment to use.
 * @param style The text style to use.
 * @param modifier The [Modifier] to be applied to the label.
 */
@Composable
fun BitwardenPolicyWarningText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Text(
        text = text,
        textAlign = textAlign,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(8.dp),
    )
}
