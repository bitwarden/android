package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dialog.row.BitwardenBasicDialogRow
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.listitem.SelectionItemData
import com.x8bit.bitwarden.ui.platform.feature.search.handlers.SearchHandlers
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import com.x8bit.bitwarden.ui.platform.feature.search.util.searchItemTestTag
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.toPersistentList

/**
 * The contents state for the search screen.
 */
@Suppress("LongMethod")
@Composable
fun SearchContent(
    viewState: SearchState.ViewState.Content,
    searchHandlers: SearchHandlers,
    searchType: SearchTypeData,
    modifier: Modifier = Modifier,
) {
    var showConfirmationDialog: ListingItemOverflowAction? by rememberSaveable {
        mutableStateOf(null)
    }
    when (val option = showConfirmationDialog) {
        is ListingItemOverflowAction.SendAction.DeleteClick -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.delete),
                message = stringResource(id = BitwardenString.are_you_sure_delete_send),
                confirmButtonText = stringResource(id = BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = {
                    showConfirmationDialog = null
                    searchHandlers.onOverflowItemClick(option)
                },
                onDismissClick = { showConfirmationDialog = null },
                onDismissRequest = { showConfirmationDialog = null },
            )
        }

        is ListingItemOverflowAction.SendAction.CopyUrlClick,
        is ListingItemOverflowAction.SendAction.EditClick,
        is ListingItemOverflowAction.SendAction.RemovePasswordClick,
        is ListingItemOverflowAction.SendAction.ShareUrlClick,
        is ListingItemOverflowAction.SendAction.ViewClick,
        is ListingItemOverflowAction.VaultAction.CopyNoteClick,
        is ListingItemOverflowAction.VaultAction.CopyNumberClick,
        is ListingItemOverflowAction.VaultAction.CopyPasswordClick,
        is ListingItemOverflowAction.VaultAction.CopyTotpClick,
        is ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
        is ListingItemOverflowAction.VaultAction.CopyUsernameClick,
        is ListingItemOverflowAction.VaultAction.EditClick,
        is ListingItemOverflowAction.VaultAction.LaunchClick,
        is ListingItemOverflowAction.VaultAction.ViewClick,
        null,
            -> Unit
    }

    var autofillSelectionOptionsItem by rememberSaveable {
        mutableStateOf<SearchState.DisplayItem?>(null)
    }
    var masterPasswordRepromptData by rememberSaveable {
        mutableStateOf<MasterPasswordRepromptData?>(null)
    }
    autofillSelectionOptionsItem?.let { item ->
        AutofillSelectionDialog(
            displayItem = item,
            onAutofillItemClick = searchHandlers.onAutofillItemClick,
            onAutofillAndSaveItemClick = searchHandlers.onAutofillAndSaveItemClick,
            onViewItemClick = searchHandlers.onItemClick,
            onMasterPasswordRepromptRequest = { masterPasswordRepromptData = it },
            onDismissRequest = { autofillSelectionOptionsItem = null },
        )
    }
    masterPasswordRepromptData?.let { data ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                searchHandlers.onMasterPasswordRepromptSubmit(password, data)
                masterPasswordRepromptData = null
            },
            onDismissRequest = {
                masterPasswordRepromptData = null
            },
        )
    }

    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
        itemsIndexed(viewState.displayItems) { index, it ->
            BitwardenListItem(
                startIcon = it.iconData,
                label = it.title,
                labelTestTag = it.titleTestTag,
                supportingLabel = it.subtitle,
                supportingLabelTestTag = it.subtitleTestTag,
                optionsTestTag = it.overflowTestTag,
                onClick = {
                    if (it.autofillSelectionOptions.isNotEmpty()) {
                        autofillSelectionOptionsItem = it
                    } else if (it.shouldDisplayMasterPasswordReprompt) {
                        masterPasswordRepromptData = MasterPasswordRepromptData.ViewItem(
                            cipherId = it.id,
                            itemType = it.itemType,
                        )
                    } else {
                        searchHandlers.onItemClick(it.id, it.itemType)
                    }
                },
                trailingLabelIcons = it.extraIconList,
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

                                    is ListingItemOverflowAction.VaultAction -> {
                                        if (option.requiresPasswordReprompt &&
                                            it.shouldDisplayMasterPasswordReprompt
                                        ) {
                                            masterPasswordRepromptData =
                                                MasterPasswordRepromptData.OverflowItem(
                                                    action = option,
                                                )
                                        } else {
                                            searchHandlers.onOverflowItemClick(option)
                                        }
                                    }

                                    else -> searchHandlers.onOverflowItemClick(option)
                                }
                            },
                        )
                    }
                    .toPersistentList(),
                cardStyle = viewState.displayItems.toListItemCardStyle(
                    index = index,
                    dividerPadding = 56.dp,
                ),
                modifier = Modifier
                    .testTag(searchType.searchItemTestTag)
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun AutofillSelectionDialog(
    displayItem: SearchState.DisplayItem,
    onAutofillItemClick: (cipherId: String) -> Unit,
    onAutofillAndSaveItemClick: (cipherId: String) -> Unit,
    onViewItemClick: (id: String, type: SearchState.DisplayItem.ItemType) -> Unit,
    onMasterPasswordRepromptRequest: (MasterPasswordRepromptData) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val selectionCallback: (SearchState.DisplayItem, MasterPasswordRepromptData) -> Unit =
        { item, data ->
            onDismissRequest()
            if (item.shouldDisplayMasterPasswordReprompt) {
                onMasterPasswordRepromptRequest(data)
            } else {
                when (data) {
                    is MasterPasswordRepromptData.Autofill -> {
                        onAutofillItemClick(data.cipherId)
                    }

                    is MasterPasswordRepromptData.AutofillAndSave -> {
                        onAutofillAndSaveItemClick(data.cipherId)
                    }

                    is MasterPasswordRepromptData.OverflowItem -> Unit
                    is MasterPasswordRepromptData.ViewItem -> {
                        onViewItemClick(data.cipherId, data.itemType)
                    }
                }
            }
        }
    BitwardenSelectionDialog(
        title = stringResource(id = BitwardenString.autofill_or_view),
        onDismissRequest = onDismissRequest,
        selectionItems = {
            if (AutofillSelectionOption.AUTOFILL in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = BitwardenString.autofill_verb),
                    onClick = {
                        selectionCallback(
                            displayItem,
                            MasterPasswordRepromptData.Autofill(cipherId = displayItem.id),
                        )
                    },
                )
            }
            if (AutofillSelectionOption.AUTOFILL_AND_SAVE in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = BitwardenString.autofill_and_save),
                    onClick = {
                        selectionCallback(
                            displayItem,
                            MasterPasswordRepromptData.AutofillAndSave(cipherId = displayItem.id),
                        )
                    },
                )
            }
            if (AutofillSelectionOption.VIEW in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = BitwardenString.view),
                    onClick = {
                        selectionCallback(
                            displayItem,
                            MasterPasswordRepromptData.ViewItem(
                                cipherId = displayItem.id,
                                itemType = displayItem.itemType,
                            ),
                        )
                    },
                )
            }
        },
    )
}
