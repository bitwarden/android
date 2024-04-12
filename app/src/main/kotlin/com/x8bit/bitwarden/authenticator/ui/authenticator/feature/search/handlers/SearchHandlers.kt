package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search.handlers

import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search.ItemSearchAction
import com.x8bit.bitwarden.authenticator.ui.authenticator.feature.search.ItemSearchViewModel

/**
 * A collection of delegate functions for managing actions within the context of the search screen.
 */
class SearchHandlers(
    val onBackClick: () -> Unit,
    val onDismissRequest: () -> Unit,
    val onItemClick: (String) -> Unit,
    val onSearchTermChange: (String) -> Unit,
) {
    /**
     * Creates an instance of [SearchHandlers] by binding actions to the provided
     * [ItemSearchViewModel].
     */
    companion object {
        fun create(viewModel: ItemSearchViewModel): SearchHandlers =
            SearchHandlers(
                onBackClick = { viewModel.trySendAction(ItemSearchAction.BackClick) },
                onDismissRequest = { viewModel.trySendAction(ItemSearchAction.DismissDialogClick) },
                onItemClick = { viewModel.trySendAction(ItemSearchAction.ItemClick(it)) },
                onSearchTermChange = {
                    viewModel.trySendAction(ItemSearchAction.SearchTermChange(it))
                }
            )
    }
}
