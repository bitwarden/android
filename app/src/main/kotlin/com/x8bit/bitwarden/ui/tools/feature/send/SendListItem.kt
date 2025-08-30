package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.core.util.persistentListOfNotNull
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.listitem.SelectionItemData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * A Composable function that displays a row send item.
 *
 * @param label The primary text label to display for the item.
 * @param supportingLabel An secondary text label to display beneath the label.
 * @param startIcon The [Painter] object used to draw the icon at the start of the item.
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
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemovePasswordClick: (() -> Unit)?,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    var shouldShowDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenListItem(
        label = label,
        supportingLabel = supportingLabel,
        startIcon = startIcon,
        trailingLabelIcons = trailingLabelIcons,
        onClick = onClick,
        selectionDataList = persistentListOfNotNull(
            SelectionItemData(
                text = stringResource(id = BitwardenString.copy_link),
                onClick = onCopyClick,
            ),
            SelectionItemData(
                text = stringResource(id = BitwardenString.share_link),
                onClick = onShareClick,
            ),
            SelectionItemData(
                text = stringResource(id = BitwardenString.view),
                onClick = onViewClick,
            ),
            SelectionItemData(
                text = stringResource(id = BitwardenString.edit),
                onClick = onEditClick,
            ),
            onRemovePasswordClick?.let {
                SelectionItemData(
                    text = stringResource(id = BitwardenString.remove_password),
                    onClick = it,
                )
            },
            SelectionItemData(
                text = stringResource(id = BitwardenString.delete),
                onClick = { shouldShowDeleteConfirmationDialog = true },
            ),
        )
            // Only show options if allowed
            .filter { showMoreOptions }
            .toPersistentList(),
        optionsTestTag = "Options",
        cardStyle = cardStyle,
        modifier = modifier,
    )
    if (shouldShowDeleteConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.delete),
            message = stringResource(id = BitwardenString.are_you_sure_delete_send),
            confirmButtonText = stringResource(id = BitwardenString.yes),
            dismissButtonText = stringResource(id = BitwardenString.cancel),
            onConfirmClick = {
                shouldShowDeleteConfirmationDialog = false
                onDeleteClick()
            },
            onDismissClick = { shouldShowDeleteConfirmationDialog = false },
            onDismissRequest = { shouldShowDeleteConfirmationDialog = false },
        )
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
            onClick = {},
            onCopyClick = {},
            onViewClick = {},
            onEditClick = {},
            onShareClick = {},
            onDeleteClick = {},
            onRemovePasswordClick = null,
            cardStyle = CardStyle.Full,
        )
    }
}
