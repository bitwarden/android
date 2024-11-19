package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.components.model.toIconResources
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

private const val TOTP_TYPES_COUNT: Int = 1
private const val TRASH_TYPES_COUNT: Int = 1

/**
 * Content view for the [VaultScreen].
 */
@Composable
@Suppress("LongMethod")
fun VaultContent(
    state: VaultState.ViewState.Content,
    vaultHandlers: VaultHandlers,
    onOverflowOptionClick: (action: ListingItemOverflowAction.VaultAction) -> Unit,
    showSshKeys: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        if (state.totpItemsCount > 0) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.totp),
                    supportingLabel = TOTP_TYPES_COUNT.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = R.drawable.ic_clock),
                    label = stringResource(id = R.string.verification_codes),
                    supportingLabel = state.totpItemsCount.toString(),
                    onClick = vaultHandlers.verificationCodesClick,
                    showDivider = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("VerificationCodesFilter")
                        .padding(16.dp),
                )
            }
        }

        if (state.favoriteItems.isNotEmpty()) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.favorites),
                    supportingLabel = state.favoriteItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(state.favoriteItems) { favoriteItem ->
                VaultEntryListItem(
                    startIcon = favoriteItem.startIcon,
                    startIconTestTag = favoriteItem.startIconTestTag,
                    trailingLabelIcons = favoriteItem
                        .extraIconList
                        .toIconResources()
                        .toPersistentList(),
                    label = favoriteItem.name(),
                    supportingLabel = favoriteItem.supportingLabel?.invoke(),
                    onClick = { vaultHandlers.vaultItemClick(favoriteItem) },
                    overflowOptions = favoriteItem.overflowOptions.toImmutableList(),
                    onOverflowOptionClick = { action ->
                        if (favoriteItem.shouldShowMasterPasswordReprompt &&
                            action.requiresPasswordReprompt
                        ) {
                            onOverflowOptionClick(action)
                        } else {
                            vaultHandlers.overflowOptionClick(action)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CipherCell")
                        .padding(
                            start = 16.dp,
                            // There is some built-in padding to the menu button that makes up
                            // the visual difference here.
                            end = 12.dp,
                        ),
                )
            }

            item {
                BitwardenHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.types),
                supportingLabel = state.itemTypesCount.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = R.drawable.ic_globe),
                startIconTestTag = "LoginCipherIcon",
                label = stringResource(id = R.string.type_login),
                supportingLabel = state.loginItemsCount.toString(),
                onClick = vaultHandlers.loginGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("LoginFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = R.drawable.ic_payment_card),
                startIconTestTag = "CardCipherIcon",
                label = stringResource(id = R.string.type_card),
                supportingLabel = state.cardItemsCount.toString(),
                onClick = vaultHandlers.cardGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("CardFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = R.drawable.ic_id_card),
                startIconTestTag = "IdentityCipherIcon",
                label = stringResource(id = R.string.type_identity),
                supportingLabel = state.identityItemsCount.toString(),
                onClick = vaultHandlers.identityGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("IdentityFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = R.drawable.ic_note),
                startIconTestTag = "SecureNoteCipherIcon",
                label = stringResource(id = R.string.type_secure_note),
                supportingLabel = state.secureNoteItemsCount.toString(),
                onClick = vaultHandlers.secureNoteGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SecureNoteFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        if (showSshKeys) {
            item {
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = R.drawable.ic_ssh_key),
                    startIconTestTag = "SshKeyCipherIcon",
                    label = stringResource(id = R.string.type_ssh_key),
                    supportingLabel = state.sshKeyItemsCount.toString(),
                    onClick = vaultHandlers.sshKeyGroupClick,
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("SshKeyFilter")
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (state.folderItems.isNotEmpty()) {
            item {
                BitwardenHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.folders),
                    supportingLabel = state.folderItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(state.folderItems) { folder ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = R.drawable.ic_folder),
                    label = folder.name(),
                    supportingLabel = folder.itemCount.toString(),
                    onClick = { vaultHandlers.folderClick(folder) },
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("FolderFilter")
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (state.noFolderItems.isNotEmpty()) {
            item {
                BitwardenHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.folder_none),
                    supportingLabel = state.noFolderItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            items(state.noFolderItems) { noFolderItem ->
                VaultEntryListItem(
                    startIcon = noFolderItem.startIcon,
                    startIconTestTag = noFolderItem.startIconTestTag,
                    trailingLabelIcons = noFolderItem
                        .extraIconList
                        .toIconResources()
                        .toPersistentList(),
                    label = noFolderItem.name(),
                    supportingLabel = noFolderItem.supportingLabel?.invoke(),
                    onClick = { vaultHandlers.vaultItemClick(noFolderItem) },
                    overflowOptions = noFolderItem.overflowOptions.toImmutableList(),
                    onOverflowOptionClick = { action ->
                        if (noFolderItem.shouldShowMasterPasswordReprompt &&
                            action.requiresPasswordReprompt
                        ) {
                            onOverflowOptionClick(action)
                        } else {
                            vaultHandlers.overflowOptionClick(action)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CipherCell")
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (state.collectionItems.isNotEmpty()) {
            item {
                BitwardenHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.collections),
                    supportingLabel = state.collectionItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            items(state.collectionItems) { collection ->
                BitwardenGroupItem(
                    startIcon = rememberVectorPainter(id = R.drawable.ic_collections),
                    label = collection.name,
                    supportingLabel = collection.itemCount.toString(),
                    onClick = { vaultHandlers.collectionClick(collection) },
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("CollectionFilter")
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            BitwardenHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            )
        }

        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.trash),
                supportingLabel = TRASH_TYPES_COUNT.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            BitwardenGroupItem(
                startIcon = rememberVectorPainter(id = R.drawable.ic_trash),
                label = stringResource(id = R.string.trash),
                supportingLabel = state.trashItemsCount.toString(),
                onClick = vaultHandlers.trashClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TrashFilter")
                    .padding(horizontal = 16.dp),
            )
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
