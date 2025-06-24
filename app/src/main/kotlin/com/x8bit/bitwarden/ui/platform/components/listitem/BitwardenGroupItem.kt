package com.x8bit.bitwarden.ui.platform.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A reusable composable function that displays a group item.
 * The list item consists of a start icon, a label, a supporting label and an optional divider.
 *
 * @param label The main text label to be displayed in the group item.
 * @param supportingLabel The secondary supporting text label to be displayed beside the label.
 * @param startIcon The [Painter] object used to draw the icon at the start of the group item.
 * @param onClick A lambda function that is invoked when the group is clicked.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier The [Modifier] to be applied to the [Row] composable that holds the list item.
 * @param showDivider Indicates whether the divider should be shown or not.
 * @param startIconTestTag The optional test tag for the [startIcon].
 */
@Composable
fun BitwardenGroupItem(
    label: String,
    supportingLabel: String,
    startIcon: Painter,
    onClick: () -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    startIconTestTag: String? = null,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onClick,
                paddingHorizontal = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = startIcon,
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .nullableTestTag(tag = startIconTestTag)
                .size(size = 24.dp),
        )

        Text(
            text = label,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = supportingLabel,
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
    }
}

@Preview
@Composable
private fun BitwardenGroupItem_preview() {
    BitwardenTheme {
        BitwardenGroupItem(
            label = "Sample Label",
            supportingLabel = "5",
            startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_file_text),
            startIconTestTag = "Test Tag",
            onClick = {},
            cardStyle = CardStyle.Full,
        )
    }
}
