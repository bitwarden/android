package com.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.button.color.bitwardenTextButtonColors
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.util.throttledClick
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled [TextButton].
 *
 * @param buttonData The data for the button.
 * @param modifier The [Modifier] to be applied to the button.
 * @param contentColor The color for the label text and icon.
 */
@Composable
fun BitwardenTextButton(
    buttonData: BitwardenButtonData,
    modifier: Modifier = Modifier,
    contentColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
) {
    BitwardenTextButton(
        label = buttonData.label(),
        onClick = buttonData.onClick,
        icon = buttonData.icon,
        isExternalLink = buttonData.isExternalLink,
        isEnabled = buttonData.isEnabled,
        contentColor = contentColor,
        modifier = modifier.nullableTestTag(tag = buttonData.testTag),
    )
}

/**
 * Represents a Bitwarden-styled [TextButton].
 *
 * @param label The label for the button.
 * @param onClick The callback when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param icon The icon for the button.
 * @param isEnabled Whether the button is enabled.
 * @param isExternalLink Indicates that this button launches an external link.
 * @param contentColor The color for the label text and icon.
 */
@Composable
fun BitwardenTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    isEnabled: Boolean = true,
    isExternalLink: Boolean = false,
    contentColor: Color = BitwardenTheme.colorScheme.outlineButton.foreground,
) {
    val formattedContentDescription = if (isExternalLink) {
        stringResource(
            id = BitwardenString.external_link_format,
            formatArgs = arrayOf(label),
        )
    } else {
        label
    }
    TextButton(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = formattedContentDescription
        },
        onClick = throttledClick(onClick = onClick),
        enabled = isEnabled,
        contentPadding = PaddingValues(
            top = 10.dp,
            bottom = 10.dp,
            start = 12.dp,
            end = if (icon == null) 12.dp else 16.dp,
        ),
        colors = bitwardenTextButtonColors(contentColor = contentColor),
    ) {
        icon?.let {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = BitwardenTheme.typography.labelLarge,
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
    }
}

@Preview
@Composable
private fun BitwardenTextButton_preview() {
    Column {
        BitwardenTextButton(
            label = "Label",
            onClick = {},
            isEnabled = true,
        )
        BitwardenTextButton(
            label = "Label",
            onClick = {},
            isEnabled = false,
        )
    }
}
