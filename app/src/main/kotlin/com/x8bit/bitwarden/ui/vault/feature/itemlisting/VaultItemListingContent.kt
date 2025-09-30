package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.listitem.SelectionItemData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.toPersistentList

/**
 * Content view for the [VaultItemListingScreen].
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultItemListingContent(
    state: VaultItemListingState.ViewState.Content,
    policyDisablesSend: Boolean,
    showAddTotpBanner: Boolean,
    collectionClick: (id: String) -> Unit,
    folderClick: (id: String) -> Unit,
    vaultItemClick: (id: String, type: VaultItemListingState.DisplayItem.ItemType) -> Unit,
    masterPasswordRepromptSubmit: (password: String, data: MasterPasswordRepromptData) -> Unit,
    onOverflowItemClick: (action: ListingItemOverflowAction) -> Unit,
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
                    onOverflowItemClick(option)
                },
                onDismissClick = { showConfirmationDialog = null },
                onDismissRequest = { showConfirmationDialog = null },
            )
        }

        is ListingItemOverflowAction.SendAction.CopyUrlClick,
        is ListingItemOverflowAction.SendAction.EditClick,
        is ListingItemOverflowAction.SendAction.ViewClick,
        is ListingItemOverflowAction.SendAction.RemovePasswordClick,
        is ListingItemOverflowAction.SendAction.ShareUrlClick,
        is ListingItemOverflowAction.VaultAction.CopyNoteClick,
        is ListingItemOverflowAction.VaultAction.CopyNumberClick,
        is ListingItemOverflowAction.VaultAction.CopyPasswordClick,
        is ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
        is ListingItemOverflowAction.VaultAction.CopyUsernameClick,
        is ListingItemOverflowAction.VaultAction.EditClick,
        is ListingItemOverflowAction.VaultAction.LaunchClick,
        is ListingItemOverflowAction.VaultAction.ViewClick,
        is ListingItemOverflowAction.VaultAction.CopyTotpClick,
        null,
            -> Unit
    }

    var masterPasswordRepromptData by remember { mutableStateOf<MasterPasswordRepromptData?>(null) }
    masterPasswordRepromptData?.let { data ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                masterPasswordRepromptData = null
                masterPasswordRepromptSubmit(password, data)
            },
            onDismissRequest = {
                masterPasswordRepromptData = null
            },
        )
    }

    LazyColumn(
        modifier = modifier,
    ) {
        if (showAddTotpBanner) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
                BitwardenInfoCalloutCard(
                    text = stringResource(
                        id = BitwardenString.add_this_authenticator_key_to_a_login,
                    ),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
        }

        if (policyDisablesSend) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
                BitwardenInfoCalloutCard(
                    text = stringResource(id = BitwardenString.send_disabled_warning),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth(),
                )
            }
        }

        if (state.displayCollectionList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.collections),
                    supportingLabel = state.displayCollectionList.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(state.displayCollectionList) { index, collection ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_collections),
                    label = collection.name,
                    supportingLabel = collection.count.toString(),
                    onClick = { collectionClick(collection.id) },
                    showDivider = false,
                    cardStyle = state
                        .displayCollectionList
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        if (state.displayFolderList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.folders),
                    supportingLabel = state.displayFolderList.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(state.displayFolderList) { index, folder ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_folder),
                    label = folder.name,
                    supportingLabel = folder.count.toString(),
                    onClick = { folderClick(folder.id) },
                    showDivider = false,
                    cardStyle = state
                        .displayFolderList
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        if (state.displayItemList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(height = 12.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.items),
                    supportingLabel = state.displayItemList.size.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            itemsIndexed(state.displayItemList) { index, it ->
                BitwardenListItem(
                    startIcon = it.iconData,
                    startIconTestTag = it.iconTestTag,
                    label = it.title.invoke(),
                    labelTestTag = it.titleTestTag,
                    secondSupportingLabel = it.secondSubtitle,
                    secondSupportingLabelTestTag = it.secondSubtitleTestTag,
                    supportingLabel = it.subtitle,
                    supportingLabelTestTag = it.subtitleTestTag,
                    optionsTestTag = it.optionsTestTag,
                    onClick = {
                        if (it.isAutofill && it.shouldShowMasterPasswordReprompt) {
                            masterPasswordRepromptData = MasterPasswordRepromptData.Autofill(
                                cipherId = it.id,
                            )
                        } else if (it.shouldShowMasterPasswordReprompt) {
                            masterPasswordRepromptData = MasterPasswordRepromptData.ViewItem(
                                id = it.id,
                                itemType = it.itemType,
                            )
                        } else {
                            vaultItemClick(it.id, it.itemType)
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
                                                it.shouldShowMasterPasswordReprompt
                                            ) {
                                                masterPasswordRepromptData =
                                                    MasterPasswordRepromptData.OverflowItem(
                                                        action = option,
                                                    )
                                            } else {
                                                onOverflowItemClick(option)
                                            }
                                        }

                                        else -> onOverflowItemClick(option)
                                    }
                                },
                            )
                        }
                        // Only show options if allowed
                        .filter { !policyDisablesSend }
                        .toPersistentList(),
                    cardStyle = state
                        .displayItemList
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
