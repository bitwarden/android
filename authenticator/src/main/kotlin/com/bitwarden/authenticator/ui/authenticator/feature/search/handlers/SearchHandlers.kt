package com.bitwarden.authenticator.ui.authenticator.feature.search.handlers

import com.bitwarden.authenticator.ui.authenticator.feature.search.ItemSearchAction
import com.bitwarden.authenticator.ui.authenticator.feature.search.ItemSearchViewModel
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem

/**
 * A collection of delegate functions for managing actions within the context of the search screen.
 */
class SearchHandlers(
    val onBackClick: () -> Unit,
    val onItemClick: (String) -> Unit,
    val onSearchTermChange: (String) -> Unit,
    val onDropdownMenuClick: (VaultDropdownMenuAction, VerificationCodeDisplayItem) -> Unit,
    val onDismissDialog: () -> Unit,
    val onConfirmDeleteClick: (String) -> Unit,
) {
    /**
     * Creates an instance of [SearchHandlers] by binding actions to the provided
     * [ItemSearchViewModel].
     */
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [SearchHandlers] by binding actions to the provided
         * [ItemSearchViewModel].
         */
        fun create(viewModel: ItemSearchViewModel): SearchHandlers =
            SearchHandlers(
                onBackClick = { viewModel.trySendAction(ItemSearchAction.BackClick) },
                onItemClick = { viewModel.trySendAction(ItemSearchAction.ItemClick(it)) },
                onSearchTermChange = {
                    viewModel.trySendAction(ItemSearchAction.SearchTermChange(it))
                },
                onDropdownMenuClick = { action, item ->
                    viewModel.trySendAction(ItemSearchAction.DropdownMenuClick(action, item))
                },
                onDismissDialog = { viewModel.trySendAction(ItemSearchAction.DismissDialog) },
                onConfirmDeleteClick = {
                    viewModel.trySendAction(ItemSearchAction.ConfirmDeleteClick(it))
                },
            )
    }
}
