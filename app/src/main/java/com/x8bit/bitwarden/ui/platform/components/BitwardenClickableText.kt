package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    style: TextStyle,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues = PaddingValues(vertical = 4.dp, horizontal = 16.dp),
    cornerSize: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(cornerSize))
            .clickable(
                indication = rememberRipple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary,
                ),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(innerPadding),
        text = label,
        textAlign = TextAlign.Start,
        color = color,
        style = style,
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
