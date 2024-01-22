package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SearchViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(initialState = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(searchType = SearchType.Sends.All)
        val viewModel = createViewModel(initialState = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SearchAction.BackClick)
            assertEquals(SearchEvent.NavigateBack, awaitItem())
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createViewModel(
        initialState: SearchState? = null,
    ): SearchViewModel = SearchViewModel(
        SavedStateHandle().apply {
            set("state", initialState)
            set(
                "search_type",
                when (initialState?.searchType) {
                    SearchType.Sends.All -> "search_type_sends_all"
                    SearchType.Sends.Files -> "search_type_sends_file"
                    SearchType.Sends.Texts -> "search_type_sends_text"
                    SearchType.Vault.All -> "search_type_vault_all"
                    SearchType.Vault.Cards -> "search_type_vault_cards"
                    is SearchType.Vault.Collection -> "search_type_vault_collection"
                    is SearchType.Vault.Folder -> "search_type_vault_folder"
                    SearchType.Vault.Identities -> "search_type_vault_identities"
                    SearchType.Vault.Logins -> "search_type_vault_logins"
                    SearchType.Vault.NoFolder -> "search_type_vault_no_folder"
                    SearchType.Vault.SecureNotes -> "search_type_vault_secure_notes"
                    SearchType.Vault.Trash -> "search_type_vault_trash"
                    null -> "search_type_vault_all"
                },
            )
            set(
                "search_type_id",
                when (val searchType = initialState?.searchType) {
                    SearchType.Sends.All -> null
                    SearchType.Sends.Files -> null
                    SearchType.Sends.Texts -> null
                    SearchType.Vault.All -> null
                    SearchType.Vault.Cards -> null
                    is SearchType.Vault.Collection -> searchType.collectionId
                    is SearchType.Vault.Folder -> searchType.folderId
                    SearchType.Vault.Identities -> null
                    SearchType.Vault.Logins -> null
                    SearchType.Vault.NoFolder -> null
                    SearchType.Vault.SecureNotes -> null
                    SearchType.Vault.Trash -> null
                    null -> null
                },
            )
        },
    )
}

private val DEFAULT_STATE: SearchState = SearchState(
    searchType = SearchType.Vault.All,
)
