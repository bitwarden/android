package com.x8bit.bitwarden.ui.platform.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A Composable function that displays a row item.
 *
 * @param label The primary text label to display for the item.
 * @param startIcon The [Painter] object used to draw the icon at the start of the item.
 * @param onClick The lambda to be invoked when the item is clicked.
 * @param selectionDataList A list of all the selection items to be displayed in the overflow
 * dialog.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 * @param labelTestTag The optional test tag for the [label].
 * @param optionsTestTag The optional test tag for the options button.
 * @param secondSupportingLabel An additional optional text label to display beneath the label and
 * above the optional supporting label.
 * @param secondSupportingLabelTestTag The optional test tag for the [secondSupportingLabel].
 * @param supportingLabel An optional secondary text label to display beneath the label.
 * @param supportingLabelTestTag The optional test tag for the [supportingLabel].
 * @param startIconTestTag The optional test tag for the [startIcon].
 * @param trailingLabelIcons An optional list of small icons to be displayed after the [label].
 */
@Suppress("LongMethod")
@Composable
fun BitwardenListItem(
    label: String,
    startIcon: IconData,
    onClick: () -> Unit,
    selectionDataList: ImmutableList<SelectionItemData>,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    labelTestTag: String? = null,
    optionsTestTag: String? = null,
    secondSupportingLabel: String? = null,
    secondSupportingLabelTestTag: String? = null,
    supportingLabel: String? = null,
    supportingLabelTestTag: String? = null,
    startIconTestTag: String? = null,
    trailingLabelIcons: ImmutableList<IconData> = persistentListOf(),
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .cardStyle(
                cardStyle = cardStyle,
                onClick = onClick,
                paddingStart = 16.dp,
                paddingEnd = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .nullableTestTag(tag = startIconTestTag)
                .size(size = 24.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = BitwardenTheme.colorScheme.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .nullableTestTag(tag = labelTestTag)
                        .weight(weight = 1f, fill = false),
                )

                trailingLabelIcons.forEach { iconData ->
                    Spacer(modifier = Modifier.width(8.dp))
                    BitwardenIcon(
                        iconData = iconData,
                        tint = BitwardenTheme.colorScheme.icon.primary,
                        modifier = Modifier.size(size = 16.dp),
                    )
                }
            }

            secondSupportingLabel.orNullIfBlank()?.let { secondSupportLabel ->
                Text(
                    text = secondSupportLabel,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.nullableTestTag(tag = secondSupportingLabelTestTag),
                )
            }

            supportingLabel.orNullIfBlank()?.let { supportLabel ->
                Text(
                    text = supportLabel,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.nullableTestTag(tag = supportingLabelTestTag),
                )
            }
        }

        if (selectionDataList.isNotEmpty()) {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_ellipsis_horizontal,
                contentDescription = stringResource(id = BitwardenString.options),
                onClick = { shouldShowDialog = true },
                modifier = Modifier.nullableTestTag(tag = optionsTestTag),
            )
        }
    }

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = label,
            onDismissRequest = { shouldShowDialog = false },
            selectionItems = {
                selectionDataList.forEach { itemData ->
                    BitwardenBasicDialogRow(
                        modifier = Modifier.testTag("AlertSelectionOption"),
                        text = itemData.text,
                        onClick = {
                            shouldShowDialog = false
                            itemData.onClick()
                        },
                    )
                }
            },
        )
    }
}

/**
 * Wrapper for the an individual selection item's data.
 */
data class SelectionItemData(
    val text: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

@Preview
@Composable
private fun BitwardenListItem_preview() {
    BitwardenTheme {
        BitwardenListItem(
            label = "Sample Label",
            supportingLabel = "Jan 3, 2024, 10:35 AM",
            startIcon = IconData.Local(BitwardenDrawable.ic_file_text),
            onClick = {},
            selectionDataList = persistentListOf(),
            cardStyle = CardStyle.Full,
        )
    }
}
