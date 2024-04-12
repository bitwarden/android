package com.x8bit.bitwarden.authenticator.ui.platform.components.listitem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.R
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.authenticator.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.x8bit.bitwarden.authenticator.ui.platform.components.icon.BitwardenIcon
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
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
 * @param modifier An optional [Modifier] for this Composable, defaulting to an empty Modifier.
 * This allows the caller to specify things like padding, size, etc.
 * @param labelTestTag The optional test tag for the [label].
 * @param optionsTestTag The optional test tag for the options button.
 * @param supportingLabel An optional secondary text label to display beneath the label.
 * @param supportingLabelTestTag The optional test tag for the [supportingLabel].
 * @param trailingLabelIcons An optional list of small icons to be displayed after the [label].
 */
@Suppress("LongMethod")
@Composable
fun BitwardenListItem(
    label: String,
    startIcon: IconData,
    onClick: () -> Unit,
    selectionDataList: ImmutableList<SelectionItemData>,
    modifier: Modifier = Modifier,
    labelTestTag: String? = null,
    optionsTestTag: String? = null,
    supportingLabel: String? = null,
    supportingLabelTestTag: String? = null,
    trailingLabelIcons: ImmutableList<IconResource> = persistentListOf(),
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 72.dp)
            .padding(vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BitwardenIcon(
            iconData = startIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .semantics { labelTestTag?.let { testTag = it } }
                        .weight(weight = 1f, fill = false),
                )

                trailingLabelIcons.forEach { iconResource ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = iconResource.iconPainter,
                        contentDescription = iconResource.contentDescription,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .semantics { iconResource.testTag?.let { testTag = it } }
                            .size(16.dp),
                    )
                }
            }

            supportingLabel?.let { supportLabel ->
                Text(
                    text = supportLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { supportingLabelTestTag?.let { testTag = it } },
                )
            }
        }

        if (selectionDataList.isNotEmpty()) {
            IconButton(
                onClick = { shouldShowDialog = true },
                modifier = Modifier.semantics { optionsTestTag?.let { testTag = it } },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_horizontal),
                    contentDescription = stringResource(id = R.string.options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = label,
            onDismissRequest = { shouldShowDialog = false },
            selectionItems = {
                selectionDataList.forEach { itemData ->
                    BitwardenBasicDialogRow(
                        modifier = Modifier.semantics { itemData.testTag?.let { testTag = it } },
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

@Preview(showBackground = true)
@Composable
private fun BitwardenListItem_preview() {
    AuthenticatorTheme {
        BitwardenListItem(
            label = "Sample Label",
            supportingLabel = "Jan 3, 2024, 10:35 AM",
            startIcon = IconData.Local(R.drawable.ic_login_item),
            onClick = {},
            selectionDataList = persistentListOf(),
        )
    }
}
