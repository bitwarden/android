package com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers

import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction

/**
 * A collection of handler functions for managing actions within the context of viewing a list of
 * items.
 */
data class VaultItemListingHandlers(
    val backClick: () -> Unit,
    val searchIconClick: () -> Unit,
    val addVaultItemClick: () -> Unit,
    val itemClick: (id: String) -> Unit,
    val refreshClick: () -> Unit,
    val syncClick: () -> Unit,
    val lockClick: () -> Unit,
    val overflowItemClick: (action: VaultItemListingsAction) -> Unit,
) {
    companion object {
        /**
         * Creates an instance of [VaultItemListingHandlers] by binding actions to the provided
         * [VaultItemListingViewModel].
         */
        fun create(
            viewModel: VaultItemListingViewModel,
        ): VaultItemListingHandlers =
            VaultItemListingHandlers(
                backClick = { viewModel.trySendAction(VaultItemListingsAction.BackClick) },
                searchIconClick = {
                    viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
                },
                addVaultItemClick = {
                    viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                },
                itemClick = { viewModel.trySendAction(VaultItemListingsAction.ItemClick(it)) },
                refreshClick = { viewModel.trySendAction(VaultItemListingsAction.RefreshClick) },
                syncClick = { viewModel.trySendAction(VaultItemListingsAction.SyncClick) },
                lockClick = { viewModel.trySendAction(VaultItemListingsAction.LockClick) },
                overflowItemClick = { viewModel.trySendAction(it) },
            )
    }
}
