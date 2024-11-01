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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.listitem.SelectionItemData
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
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
 * @param onEditClick The lambda to be invoked when the edit option is clicked from the menu.
 * @param onCopyClick The lambda to be invoked when the copy option is clicked from the menu.
 * @param onShareClick The lambda to be invoked when the share option is clicked from the menu.
 * @param onDeleteClick The lambda to be invoked when the delete option is clicked from the menu.
 * @param onRemovePasswordClick The lambda to be invoked when the remove password option is clicked
 * from the menu, if `null` the remove password button is not displayed.
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 */
@Suppress("LongMethod")
@Composable
fun SendListItem(
    label: String,
    supportingLabel: String,
    startIcon: IconData,
    trailingLabelIcons: ImmutableList<IconRes>,
    showMoreOptions: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemovePasswordClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var shouldShowDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenListItem(
        label = label,
        supportingLabel = supportingLabel,
        startIcon = startIcon,
        trailingLabelIcons = trailingLabelIcons
            .map {
                IconResource(
                    iconPainter = rememberVectorPainter(it.iconRes),
                    contentDescription = it.contentDescription(),
                )
            }
            .toPersistentList(),
        onClick = onClick,
        selectionDataList = persistentListOfNotNull(
            SelectionItemData(
                text = stringResource(id = R.string.edit),
                onClick = onEditClick,
            ),
            SelectionItemData(
                text = stringResource(id = R.string.copy_link),
                onClick = onCopyClick,
            ),
            SelectionItemData(
                text = stringResource(id = R.string.share_link),
                onClick = onShareClick,
            ),
            onRemovePasswordClick?.let {
                SelectionItemData(
                    text = stringResource(id = R.string.remove_password),
                    onClick = it,
                )
            },
            SelectionItemData(
                text = stringResource(id = R.string.delete),
                onClick = { shouldShowDeleteConfirmationDialog = true },
            ),
        )
            // Only show options if allowed
            .filter { showMoreOptions }
            .toPersistentList(),
        optionsTestTag = "Options",
        modifier = modifier,
    )
    if (shouldShowDeleteConfirmationDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.delete),
            message = stringResource(id = R.string.are_you_sure_delete_send),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.cancel),
            onConfirmClick = {
                shouldShowDeleteConfirmationDialog = false
                onDeleteClick()
            },
            onDismissClick = { shouldShowDeleteConfirmationDialog = false },
            onDismissRequest = { shouldShowDeleteConfirmationDialog = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SendListItem_preview() {
    BitwardenTheme {
        SendListItem(
            label = "Sample Label",
            supportingLabel = "Jan 3, 2024, 10:35 AM",
            startIcon = IconData.Local(R.drawable.ic_file_text),
            trailingLabelIcons = persistentListOf(),
            showMoreOptions = true,
            onClick = {},
            onCopyClick = {},
            onEditClick = {},
            onShareClick = {},
            onDeleteClick = {},
            onRemovePasswordClick = null,
        )
    }
}
