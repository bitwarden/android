package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderTextWithSupportLabel
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers

/**
 * Content view for the [VaultScreen].
 */
@Composable
@Suppress("LongMethod")
fun VaultContent(
    state: VaultState.ViewState.Content,
    vaultHandlers: VaultHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        if (state.totpItemsCount > 0) {

            item {
                BitwardenListHeaderTextWithSupportLabel(
                    label = stringResource(id = R.string.totp),
                    supportingLabel = "1",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item {
                BitwardenGroupItem(
                    startIcon = painterResource(id = R.drawable.access_time),
                    label = stringResource(id = R.string.verification_codes),
                    supportingLabel = state.totpItemsCount.toString(),
                    onClick = vaultHandlers.verificationCodesClick,
                    showDivider = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        if (state.favoriteItems.isNotEmpty()) {

            item {
                BitwardenListHeaderTextWithSupportLabel(
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
                    label = favoriteItem.name(),
                    supportingLabel = favoriteItem.supportingLabel?.invoke(),
                    onClick = { vaultHandlers.vaultItemClick(favoriteItem) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            // There is some built-in padding to the menu button that makes up
                            // the visual difference here.
                            end = 12.dp,
                        ),
                )
            }

            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }
        }

        item {
            BitwardenListHeaderTextWithSupportLabel(
                label = stringResource(id = R.string.types),
                supportingLabel = "4",
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
                startIcon = painterResource(id = R.drawable.ic_login_item),
                label = stringResource(id = R.string.type_login),
                supportingLabel = state.loginItemsCount.toString(),
                onClick = vaultHandlers.loginGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = painterResource(id = R.drawable.ic_card_item),
                label = stringResource(id = R.string.type_card),
                supportingLabel = state.cardItemsCount.toString(),
                onClick = vaultHandlers.cardGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = painterResource(id = R.drawable.ic_identity_item),
                label = stringResource(id = R.string.type_identity),
                supportingLabel = state.identityItemsCount.toString(),
                onClick = vaultHandlers.identityGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item {
            BitwardenGroupItem(
                startIcon = painterResource(id = R.drawable.ic_secure_note_item),
                label = stringResource(id = R.string.type_secure_note),
                supportingLabel = state.secureNoteItemsCount.toString(),
                onClick = vaultHandlers.secureNoteGroupClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (state.folderItems.isNotEmpty()) {
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderTextWithSupportLabel(
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
                    startIcon = painterResource(id = R.drawable.ic_folder),
                    label = folder.name(),
                    supportingLabel = folder.itemCount.toString(),
                    onClick = { vaultHandlers.folderClick(folder) },
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (state.noFolderItems.isNotEmpty()) {
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderTextWithSupportLabel(
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
                    label = noFolderItem.name(),
                    supportingLabel = noFolderItem.supportingLabel?.invoke(),
                    onClick = { vaultHandlers.vaultItemClick(noFolderItem) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (state.collectionItems.isNotEmpty()) {
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                )
            }

            item {
                BitwardenListHeaderTextWithSupportLabel(
                    label = stringResource(id = R.string.collections),
                    supportingLabel = state.collectionItems.count().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            items(state.collectionItems) { collection ->
                BitwardenGroupItem(
                    startIcon = painterResource(id = R.drawable.ic_collection),
                    label = collection.name,
                    supportingLabel = collection.itemCount.toString(),
                    onClick = { vaultHandlers.collectionClick(collection) },
                    showDivider = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
            )
        }

        item {
            BitwardenListHeaderTextWithSupportLabel(
                label = stringResource(id = R.string.trash),
                supportingLabel = "1",
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
                startIcon = painterResource(id = R.drawable.ic_trash),
                label = stringResource(id = R.string.trash),
                supportingLabel = state.trashItemsCount.toString(),
                onClick = vaultHandlers.trashClick,
                showDivider = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
