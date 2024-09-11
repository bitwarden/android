package com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers

import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.MasterPasswordRepromptData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction

/**
 * A collection of handler functions for managing actions within the context of viewing a list of
 * items.
 */
data class VaultItemListingHandlers(
    val switchAccountClick: (AccountSummary) -> Unit,
    val lockAccountClick: (AccountSummary) -> Unit,
    val logoutAccountClick: (AccountSummary) -> Unit,
    val backClick: () -> Unit,
    val searchIconClick: () -> Unit,
    val addVaultItemClick: () -> Unit,
    val itemClick: (id: String) -> Unit,
    val folderClick: (id: String) -> Unit,
    val collectionClick: (id: String) -> Unit,
    val masterPasswordRepromptSubmit: (password: String, MasterPasswordRepromptData) -> Unit,
    val refreshClick: () -> Unit,
    val syncClick: () -> Unit,
    val lockClick: () -> Unit,
    val overflowItemClick: (action: ListingItemOverflowAction) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultItemListingHandlers] by binding actions to the provided
         * [VaultItemListingViewModel].
         */
        fun create(
            viewModel: VaultItemListingViewModel,
        ): VaultItemListingHandlers =
            VaultItemListingHandlers(
                switchAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.SwitchAccountClick(it))
                },
                lockAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.LockAccountClick(it))
                },
                logoutAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.LogoutAccountClick(it))
                },
                backClick = { viewModel.trySendAction(VaultItemListingsAction.BackClick) },
                searchIconClick = {
                    viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
                },
                addVaultItemClick = {
                    viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                },
                collectionClick = {
                    viewModel.trySendAction(VaultItemListingsAction.CollectionClick(it))
                },
                itemClick = { viewModel.trySendAction(VaultItemListingsAction.ItemClick(it)) },
                folderClick = { viewModel.trySendAction(VaultItemListingsAction.FolderClick(it)) },
                masterPasswordRepromptSubmit = { password, data ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.MasterPasswordRepromptSubmit(
                            password = password,
                            masterPasswordRepromptData = data,
                        ),
                    )
                },
                refreshClick = { viewModel.trySendAction(VaultItemListingsAction.RefreshClick) },
                syncClick = { viewModel.trySendAction(VaultItemListingsAction.SyncClick) },
                lockClick = { viewModel.trySendAction(VaultItemListingsAction.LockClick) },
                overflowItemClick = {
                    viewModel.trySendAction(VaultItemListingsAction.OverflowOptionClick(it))
                },
            )
    }
}
