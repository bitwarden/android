package com.x8bit.bitwarden.ui.platform.feature.search.handlers

import com.x8bit.bitwarden.ui.platform.feature.search.MasterPasswordRepromptData
import com.x8bit.bitwarden.ui.platform.feature.search.SearchAction
import com.x8bit.bitwarden.ui.platform.feature.search.SearchViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType

/**
 * A collection of handler functions for managing actions within the context of the search screen.
 */
data class SearchHandlers(
    val onBackClick: () -> Unit,
    val onDismissRequest: () -> Unit,
    val onItemClick: (String) -> Unit,
    val onAutofillItemClick: (String) -> Unit,
    val onAutofillAndSaveItemClick: (String) -> Unit,
    val onMasterPasswordRepromptSubmit: (password: String, MasterPasswordRepromptData) -> Unit,
    val onSearchTermChange: (String) -> Unit,
    val onVaultFilterSelect: (VaultFilterType) -> Unit,
    val onOverflowItemClick: (ListingItemOverflowAction) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [SearchHandlers] by binding actions to the provided
         * [SearchViewModel].
         */
        fun create(viewModel: SearchViewModel): SearchHandlers =
            SearchHandlers(
                onBackClick = { viewModel.trySendAction(SearchAction.BackClick) },
                onDismissRequest = { viewModel.trySendAction(SearchAction.DismissDialogClick) },
                onItemClick = { viewModel.trySendAction(SearchAction.ItemClick(it)) },
                onAutofillItemClick = {
                    viewModel.trySendAction(SearchAction.AutofillItemClick(it))
                },
                onAutofillAndSaveItemClick = {
                    viewModel.trySendAction(SearchAction.AutofillAndSaveItemClick(it))
                },
                onMasterPasswordRepromptSubmit = { password, data ->
                    viewModel.trySendAction(
                        SearchAction.MasterPasswordRepromptSubmit(
                            password = password,
                            masterPasswordRepromptData = data,
                        ),
                    )
                },
                onSearchTermChange = { viewModel.trySendAction(SearchAction.SearchTermChange(it)) },
                onVaultFilterSelect = {
                    viewModel.trySendAction(SearchAction.VaultFilterSelect(it))
                },
                onOverflowItemClick = {
                    viewModel.trySendAction(SearchAction.OverflowOptionClick(it))
                },
            )
    }
}
