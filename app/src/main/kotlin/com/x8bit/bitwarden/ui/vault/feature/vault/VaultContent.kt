package com.x8bit.bitwarden.ui.vault.feature.vault

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
import kotlinx.collections.immutable.toImmutableList

private const val TOTP_TYPES_COUNT: Int = 1
private const val TRASH_TYPES_COUNT: Int = 1

/**
 * Content view for the [VaultScreen].
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun VaultContent(
    state: VaultState.ViewState.Content,
    vaultHandlers: VaultHandlers,
    modifier: Modifier = Modifier,
) {
    // Handles the master password prompt for the row click
    var masterPasswordRepromptItem by remember {
        mutableStateOf<VaultState.ViewState.VaultItem?>(value = null)
    }
    masterPasswordRepromptItem?.let { action ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                masterPasswordRepromptItem = null
                vaultHandlers.masterPasswordRepromptSubmit(action, password)
            },
            onDismissRequest = { masterPasswordRepromptItem = null },
        )
    }
    // Handles the master password prompt for the overflow clicks
    var overflowMasterPasswordRepromptAction by remember {
        mutableStateOf<ListingItemOverflowAction.VaultAction?>(value = null)
    }
    overflowMasterPasswordRepromptAction?.let { action ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                overflowMasterPasswordRepromptAction = null
                vaultHandlers.overflowMasterPasswordRepromptSubmit(action, password)
            },
            onDismissRequest = { overflowMasterPasswordRepromptAction = null },
        )
    }
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Spacer(modifier = Modifier.height(height = 12.dp))
        }
        if (state.totpItemsCount > 0) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.totp),
                    supportingLabel = TOTP_TYPES_COUNT.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            item {
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_clock),
                    label = stringResource(id = BitwardenString.verification_codes),
                    supportingLabel = state.totpItemsCount.toString(),
                    onClick = vaultHandlers.verificationCodesClick,
                    showDivider = false,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("VerificationCodesFilter")
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        if (state.favoriteItems.isNotEmpty()) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.favorites),
                    supportingLabel = state.favoriteItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(state.favoriteItems) { index, favoriteItem ->
                VaultEntryListItem(
                    startIcon = favoriteItem.startIcon,
                    startIconTestTag = favoriteItem.startIconTestTag,
                    trailingLabelIcons = favoriteItem.extraIconList,
                    label = favoriteItem.name(),
                    supportingLabel = favoriteItem.supportingLabel?.invoke(),
                    onClick = {
                        if (favoriteItem.shouldShowMasterPasswordReprompt) {
                            masterPasswordRepromptItem = favoriteItem
                        } else {
                            vaultHandlers.vaultItemClick(favoriteItem)
                        }
                    },
                    overflowOptions = favoriteItem.overflowOptions.toImmutableList(),
                    onOverflowOptionClick = { action ->
                        if (favoriteItem.shouldShowMasterPasswordReprompt &&
                            action.requiresPasswordReprompt
                        ) {
                            overflowMasterPasswordRepromptAction = action
                        } else {
                            vaultHandlers.overflowOptionClick(action)
                        }
                    },
                    cardStyle = state
                        .favoriteItems
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CipherCell")
                        .standardHorizontalMargin(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.types),
                supportingLabel = state.itemTypesCount.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_globe),
                startIconTestTag = "LoginCipherIcon",
                label = stringResource(id = BitwardenString.type_login),
                supportingLabel = state.loginItemsCount.toString(),
                onClick = vaultHandlers.loginGroupClick,
                showDivider = false,
                cardStyle = CardStyle.Top(dividerPadding = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("LoginFilter")
                    .standardHorizontalMargin(),
            )
        }

        if (state.showCardGroup) {
            item {
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_payment_card),
                    startIconTestTag = "CardCipherIcon",
                    label = stringResource(id = BitwardenString.type_card),
                    supportingLabel = state.cardItemsCount.toString(),
                    onClick = vaultHandlers.cardGroupClick,
                    showDivider = false,
                    cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CardFilter")
                        .standardHorizontalMargin(),
                )
            }
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_id_card),
                startIconTestTag = "IdentityCipherIcon",
                label = stringResource(id = BitwardenString.type_identity),
                supportingLabel = state.identityItemsCount.toString(),
                onClick = vaultHandlers.identityGroupClick,
                showDivider = false,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("IdentityFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_note),
                startIconTestTag = "SecureNoteCipherIcon",
                label = stringResource(id = BitwardenString.type_secure_note),
                supportingLabel = state.secureNoteItemsCount.toString(),
                onClick = vaultHandlers.secureNoteGroupClick,
                showDivider = false,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SecureNoteFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_ssh_key),
                startIconTestTag = "SshKeyCipherIcon",
                label = stringResource(id = BitwardenString.type_ssh_key),
                supportingLabel = state.sshKeyItemsCount.toString(),
                onClick = vaultHandlers.sshKeyGroupClick,
                showDivider = false,
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SshKeyFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 16.dp))
        }

        if (state.folderItems.isNotEmpty()) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.folders),
                    supportingLabel = state.folderItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(state.folderItems) { index, folder ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_folder),
                    label = folder.name(),
                    supportingLabel = folder.itemCount.toString(),
                    onClick = { vaultHandlers.folderClick(folder) },
                    showDivider = false,
                    cardStyle = state
                        .folderItems
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("FolderFilter")
                        .standardHorizontalMargin(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        if (state.noFolderItems.isNotEmpty()) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.folder_none),
                    supportingLabel = state.noFolderItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            itemsIndexed(state.noFolderItems) { index, noFolderItem ->
                VaultEntryListItem(
                    startIcon = noFolderItem.startIcon,
                    startIconTestTag = noFolderItem.startIconTestTag,
                    trailingLabelIcons = noFolderItem.extraIconList,
                    label = noFolderItem.name(),
                    supportingLabel = noFolderItem.supportingLabel?.invoke(),
                    onClick = {
                        if (noFolderItem.shouldShowMasterPasswordReprompt) {
                            masterPasswordRepromptItem = noFolderItem
                        } else {
                            vaultHandlers.vaultItemClick(noFolderItem)
                        }
                    },
                    overflowOptions = noFolderItem.overflowOptions.toImmutableList(),
                    onOverflowOptionClick = { action ->
                        if (noFolderItem.shouldShowMasterPasswordReprompt &&
                            action.requiresPasswordReprompt
                        ) {
                            overflowMasterPasswordRepromptAction = action
                        } else {
                            vaultHandlers.overflowOptionClick(action)
                        }
                    },
                    cardStyle = state
                        .noFolderItems
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CipherCell")
                        .standardHorizontalMargin(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        if (state.collectionItems.isNotEmpty()) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.collections),
                    supportingLabel = state.collectionItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(state.collectionItems) { index, collection ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_collections),
                    label = collection.name,
                    supportingLabel = collection.itemCount.toString(),
                    onClick = { vaultHandlers.collectionClick(collection) },
                    showDivider = false,
                    cardStyle = state
                        .collectionItems
                        .toListItemCardStyle(index = index, dividerPadding = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CollectionFilter")
                        .standardHorizontalMargin(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(height = 16.dp))
            }
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = BitwardenString.trash),
                supportingLabel = TRASH_TYPES_COUNT.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = BitwardenDrawable.ic_trash),
                label = stringResource(id = BitwardenString.trash),
                supportingLabel = state.trashItemsCount.toString(),
                onClick = vaultHandlers.trashClick,
                showDivider = false,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TrashFilter")
                    .standardHorizontalMargin(),
            )
        }

        item {
            Spacer(modifier = Modifier.height(height = 88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
