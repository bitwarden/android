package com.x8bit.bitwarden.ui.platform.components.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

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
    style: TextStyle = BitwardenTheme.typography.bodySmall,
) {
    Text(
        text = text,
        textAlign = textAlign,
        style = style,
        color = BitwardenTheme.colorScheme.text.primary,
        modifier = modifier
            .background(
                color = BitwardenTheme.colorScheme.background.tertiary,
                shape = RoundedCornerShape(size = 8.dp),
            )
            .padding(all = 16.dp),
    )
}

@Preview
@Composable
private fun BitwardenPolicyWarningText_preview() {
    BitwardenPolicyWarningText(
        text = "text",
    )
}
