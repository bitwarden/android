package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderTextWithSupportLabel
import com.x8bit.bitwarden.ui.platform.components.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.SelectionItemData
import com.x8bit.bitwarden.ui.platform.components.model.toIconResources
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.toPersistentList

/**
 * Content view for the [VaultItemListingScreen].
 */
@Suppress("LongMethod")
@Composable
fun VaultItemListingContent(
    state: VaultItemListingState.ViewState.Content,
    vaultItemClick: (id: String) -> Unit,
    onOverflowItemClick: (action: ListingItemOverflowAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            BitwardenListHeaderTextWithSupportLabel(
                label = stringResource(id = R.string.items),
                supportingLabel = state.displayItemList.size.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        items(state.displayItemList) {
            var showConfirmationDialog: ListingItemOverflowAction? by rememberSaveable {
                mutableStateOf(null)
            }
            BitwardenListItem(
                startIcon = it.iconData,
                label = it.title,
                supportingLabel = it.subtitle,
                onClick = { vaultItemClick(it.id) },
                trailingLabelIcons = it
                    .extraIconList
                    .toIconResources()
                    .toPersistentList(),
                selectionDataList = it
                    .overflowOptions
                    .map { option ->
                        SelectionItemData(
                            text = option.title(),
                            onClick = {
                                when (option) {
                                    is ListingItemOverflowAction.SendAction.DeleteClick -> {
                                        showConfirmationDialog = option
                                    }

                                    else -> onOverflowItemClick(option)
                                }
                            },
                        )
                    }
                    .toPersistentList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        // There is some built-in padding to the menu button that makes up
                        // the visual difference here.
                        end = 12.dp,
                    ),
            )
            when (val option = showConfirmationDialog) {
                is ListingItemOverflowAction.SendAction.DeleteClick -> {
                    BitwardenTwoButtonDialog(
                        title = stringResource(id = R.string.delete),
                        message = stringResource(id = R.string.are_you_sure_delete_send),
                        confirmButtonText = stringResource(id = R.string.yes),
                        dismissButtonText = stringResource(id = R.string.cancel),
                        onConfirmClick = {
                            showConfirmationDialog = null
                            onOverflowItemClick(option)
                        },
                        onDismissClick = { showConfirmationDialog = null },
                        onDismissRequest = { showConfirmationDialog = null },
                    )
                }

                is ListingItemOverflowAction.SendAction.CopyUrlClick,
                is ListingItemOverflowAction.SendAction.EditClick,
                is ListingItemOverflowAction.SendAction.RemovePasswordClick,
                is ListingItemOverflowAction.SendAction.ShareUrlClick,
                null,
                -> Unit
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
