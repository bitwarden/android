package com.bitwarden.ui.platform.components.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.theme.BitwardenTheme

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
    leadingIcon: Painter? = null,
    innerPadding: PaddingValues = PaddingValues(vertical = 4.dp, horizontal = 16.dp),
    isEnabled: Boolean = true,
    cornerSize: Dp = 28.dp,
    color: Color = BitwardenTheme.colorScheme.text.interaction,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape = RoundedCornerShape(size = cornerSize))
            .clickable(
                indication = ripple(color = BitwardenTheme.colorScheme.background.pressed),
                interactionSource = remember { MutableInteractionSource() },
                enabled = isEnabled,
                onClick = onClick,
            )
            .padding(paddingValues = innerPadding),
    ) {
        leadingIcon?.let {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                tint = if (isEnabled) {
                    color
                } else {
                    BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                },
                modifier = Modifier.size(size = 16.dp),
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
        }
        Text(
            text = label,
            textAlign = TextAlign.Start,
            color = color,
            style = style,
        )
    }
}

@Preview
@Composable
private fun BitwardenTextButton_preview() {
    BitwardenTextButton(
        label = "Label",
        onClick = {},
    )
}
