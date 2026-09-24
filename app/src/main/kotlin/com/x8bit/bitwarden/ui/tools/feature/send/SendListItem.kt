package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.listitem.SelectionItemData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * A Composable function that displays a row send item.
 *
 * @param label The primary text label to display for the item.
 * @param supportingLabel A secondary text label to display beneath the label.
 * @param startIcon The [Painter] object used to draw the icon at the start of the item.
 * @param isDisabled  Whether the item is disabled or not.
 * @param showMoreOptions Whether to show the button for the overflow options.
 * @param onClick The lambda to be invoked when the item is clicked.
 * @param onViewClick The lambda to be invoked when the view option is clicked from the menu.
 * @param onEditClick The lambda to be invoked when the edit option is clicked from the menu.
 * @param onCopyClick The lambda to be invoked when the copy option is clicked from the menu.
 * @param onShareClick The lambda to be invoked when the share option is clicked from the menu.
 * @param onDeleteClick The lambda to be invoked when the delete option is clicked from the menu.
 * @param onRemovePasswordClick The lambda to be invoked when the remove password option is clicked
 * from the menu, if `null` the remove password button is not displayed.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 */
@Suppress("LongMethod")
@Composable
fun SendListItem(
    label: String,
    supportingLabel: String,
    startIcon: IconData,
    trailingLabelIcons: ImmutableList<IconData>,
    showMoreOptions: Boolean,
    onClick: () -> Unit,
    onOverflowAction: (ListingItemOverflowAction.SendAction) -> Unit,
    overflowOptions: ImmutableList<ListingItemOverflowAction.SendAction>,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var speedBumpAction: ListingItemOverflowAction.SendAction? by rememberSaveable {
        mutableStateOf(null)
    }
    BitwardenListItem(
        label = label,
        supportingLabel = supportingLabel,
        startIcon = startIcon,
        trailingLabelIcons = trailingLabelIcons,
        onClick = onClick,
        selectionDataList = overflowOptions
            .map { action ->
                SelectionItemData(
                    text = action.title(),
                    onClick = {
                        action
                            .speedBump
                            ?.let { speedBumpAction = action }
                            ?: onOverflowAction(action)
                    },
                    contentDescription = action.contentDescription(),
                )
            }
            // Only show options if allowed
            .filter { showMoreOptions }
            .toPersistentList(),
        optionsTestTag = "Options",
        cardStyle = cardStyle,
        modifier = modifier,
    )
    speedBumpAction?.let { action ->
        action
            .speedBump
            ?.let { speedBump ->
                BitwardenTwoButtonDialog(
                    twoButtonDialogData = speedBump,
                    onConfirmClick = {
                        speedBumpAction = null
                        onOverflowAction(action)
                    },
                    onDismissClick = { speedBumpAction = null },
                    onDismissRequest = { speedBumpAction = null },
                )
            }
            ?: run {
                // If we somehow get here and there is no speed bump, then we should keep on going.
                speedBumpAction = null
                onOverflowAction(action)
            }
    }
}

@Preview
@Composable
private fun SendListItem_preview() {
    BitwardenTheme {
        SendListItem(
            label = "Sample Label",
            supportingLabel = "Jan 3, 2024, 10:35 AM",
            startIcon = IconData.Local(BitwardenDrawable.ic_file_text),
            trailingLabelIcons = persistentListOf(),
            showMoreOptions = true,
            overflowOptions = persistentListOf(),
            onOverflowAction = { },
            onClick = {},
            cardStyle = CardStyle.Full,
        )
    }
}
