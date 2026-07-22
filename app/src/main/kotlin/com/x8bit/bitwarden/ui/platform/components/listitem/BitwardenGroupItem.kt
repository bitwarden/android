package com.x8bit.bitwarden.ui.platform.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
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
 * A reusable composable function that displays a group item.
 * The list item consists of a start icon, label, sub-label, supporting label, and an end icon.
 *
 * @param label The main text label to be displayed in the group item.
 * @param supportingLabel The secondary supporting text label to be displayed beside the label.
 * @param startIcon The [IconData] object used to draw the icon at the start of the group item.
 * @param onClick A lambda function that is invoked when the group is clicked.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier The [Modifier] to be applied to the [Row] composable that holds the list item.
 * @param subLabel The secondary text label to be displayed in the group item.
 * @param endIcon The [IconData] object used to draw the icon at the end of the group item.
 * @param selectionDataList Optional overflow menu options shown via a more-options button.
 */
@Composable
fun BitwardenGroupItem(
    label: String,
    supportingLabel: String,
    startIcon: IconData.Local,
    onClick: () -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    endIcon: IconData.Local? = null,
    selectionDataList: ImmutableList<SelectionItemData> = persistentListOf(),
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 60.dp)
            .then(
                if (selectionDataList.isEmpty()) {
                    Modifier.cardStyle(
                        cardStyle = cardStyle,
                        onClick = onClick,
                        paddingHorizontal = 16.dp,
                    )
                } else {
                    Modifier.cardStyle(
                        cardStyle = cardStyle,
                        onClick = onClick,
                        paddingStart = 16.dp,
                        paddingEnd = 4.dp,
                    )
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            tint = BitwardenTheme.colorScheme.icon.primary,
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .size(size = 24.dp),
        )

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = label,
                style = BitwardenTheme.typography.bodyLarge,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            subLabel?.let {
                Spacer(modifier = Modifier.height(height = 2.dp))
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodyMedium,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = supportingLabel,
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.primary,
        )
        endIcon?.let {
            BitwardenIcon(
                iconData = it,
                tint = BitwardenTheme.colorScheme.icon.primary,
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .size(size = 24.dp),
            )
        }
        if (selectionDataList.isNotEmpty()) {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_ellipsis_horizontal,
                contentDescription = stringResource(id = BitwardenString.more_options),
                onClick = { shouldShowDialog = true },
                modifier = Modifier.testTag(tag = "FolderMoreOptionsButton"),
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
                        modifier = Modifier
                            .semantics { contentDescription = itemData.contentDescription }
                            .testTag(tag = "AlertSelectionOption"),
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

@Preview
@Composable
private fun BitwardenGroupItem_preview() {
    BitwardenTheme {
        BitwardenGroupItem(
            label = "Sample Label",
            supportingLabel = "5",
            startIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_file_text,
                contentDescription = null,
                testTag = "Test Tag 1",
            ),
            endIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_locked,
                contentDescription = null,
                testTag = "Test Tag 2",
            ),
            onClick = {},
            subLabel = "Sample Subtext",
            cardStyle = CardStyle.Full,
        )
    }
}
