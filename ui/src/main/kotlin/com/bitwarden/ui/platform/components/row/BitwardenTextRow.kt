package com.bitwarden.ui.platform.components.row

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a clickable row of text and can contains an optional [content] that appears to the
 * right of the [text].
 *
 * @param text The label for the row as a [String].
 * @param onClick The callback when the row is clicked.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier The modifier to be applied to the layout.
 * @param description An optional description label to be displayed below the [text].
 * @param textTestTag The optional test tag for the inner text component.
 * @param isEnabled Indicates if the row is enabled or not, a disabled row will not be clickable
 * and it's contents will be dimmed.
 * @param clickable An optional override for whether the row is clickable or not. Defaults to
 * [isEnabled].
 * @param withDivider Indicates if a divider should be drawn on the bottom of the row, defaults
 * to `false`.
 * @param tooltip The data required to display a tooltip.
 * @param content The content of the [BitwardenTextRow].
 */
@Suppress("LongMethod")
@Composable
fun BitwardenTextRow(
    text: String,
    onClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    description: AnnotatedString? = null,
    textTestTag: String? = null,
    isEnabled: Boolean = true,
    clickable: Boolean = isEnabled,
    withDivider: Boolean = false,
    tooltip: TooltipData? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onClick,
                clickEnabled = clickable,
                paddingHorizontal = 16.dp,
            )
            .semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        style = BitwardenTheme.typography.bodyLarge,
                        color = if (isEnabled) {
                            BitwardenTheme.colorScheme.text.primary
                        } else {
                            BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                        },
                        modifier = Modifier.nullableTestTag(tag = textTestTag),
                    )
                    tooltip?.let { ToolTip(tooltip = it) }
                }
                description?.let {
                    Text(
                        text = it,
                        style = BitwardenTheme.typography.bodyMedium,
                        color = if (isEnabled) {
                            BitwardenTheme.colorScheme.text.secondary
                        } else {
                            BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                        },
                    )
                }
            }
            content?.invoke()
        }
        if (withDivider) {
            BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun RowScope.ToolTip(
    tooltip: TooltipData,
) {
    Spacer(modifier = Modifier.width(width = 8.dp))
    BitwardenStandardIconButton(
        vectorIconRes = BitwardenDrawable.ic_question_circle_small,
        contentDescription = tooltip.contentDescription,
        onClick = tooltip.onClick,
        contentColor = BitwardenTheme.colorScheme.icon.secondary,
        modifier = Modifier
            .testTag(tag = "TextRowTooltip"),
    )
}

@Preview
@Composable
private fun BitwardenTextRowWithTooltipAndContent_Preview() {
    BitwardenTextRow(
        text = "Sample Text",
        onClick = {},
        cardStyle = CardStyle.Full,
        description = "This is a sample description.".toAnnotatedString(),
        textTestTag = "sampleTestTag",
        isEnabled = true,
        withDivider = false,
        tooltip = TooltipData(
            contentDescription = "Tooltip Description",
            onClick = {},
        ),
    )
}

@Preview
@Composable
private fun BitwardenTextRowWithDividerDisabled_Preview() {
    BitwardenTextRow(
        text = "Sample Text Disabled",
        onClick = {},
        cardStyle = CardStyle.Top(),
        description = "This is a sample disabled description.".toAnnotatedString(),
        textTestTag = "sampleDisabledTestTag",
        isEnabled = false,
        withDivider = true,
        tooltip = null,
    )
}
