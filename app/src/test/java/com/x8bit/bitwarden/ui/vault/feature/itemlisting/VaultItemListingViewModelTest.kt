package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultItemListingViewModelTest : BaseViewModelTest() {

    private val initialState = createVaultItemListingState()
    private val initialSavedStateHandle = createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType = VaultItemListingType.Login,
    )

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                initialState, awaitItem(),
            )
        }
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.BackClick)
            assertEquals(VaultItemListingEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.SearchIconClick)
            assertEquals(VaultItemListingEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }

    @Test
    fun `ItemClick should emit NavigateToVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.ItemClick(id = "mock"))
            assertEquals(VaultItemListingEvent.NavigateToVaultItem(id = "mock"), awaitItem())
        }
    }

    @Test
    fun `AddVaultItemClick should emit NavigateToAddVaultItem`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.AddVaultItemClick)
            assertEquals(VaultItemListingEvent.NavigateToAddVaultItem, awaitItem())
        }
    }

    @Test
    fun `RefreshClick should emit ShowToast`() = runTest {
        val viewModel = createVaultItemListingViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultItemListingsAction.RefreshClick)
            assertEquals(
                VaultItemListingEvent.ShowToast("Not yet implemented".asText()),
                awaitItem(),
            )
        }
    }

    private fun createSavedStateHandleWithVaultItemListingType(
        vaultItemListingType: VaultItemListingType,
    ) = SavedStateHandle().apply {
        set(
            "vault_item_listing_type",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> "card"
                is VaultItemListingType.Folder -> "folder"
                is VaultItemListingType.Identity -> "identity"
                is VaultItemListingType.Login -> "login"
                is VaultItemListingType.SecureNote -> "secure_note"
                is VaultItemListingType.Trash -> "trash"
            },
        )
        set(
            "id",
            when (vaultItemListingType) {
                is VaultItemListingType.Card -> null
                is VaultItemListingType.Folder -> vaultItemListingType.folderId
                is VaultItemListingType.Identity -> null
                is VaultItemListingType.Login -> null
                is VaultItemListingType.SecureNote -> null
                is VaultItemListingType.Trash -> null
            },
        )
    }

    private fun createVaultItemListingViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
    ): VaultItemListingViewModel =
        VaultItemListingViewModel(
            savedStateHandle = savedStateHandle,
        )

    @Suppress("MaxLineLength")
    private fun createVaultItemListingState(
        itemListingType: VaultItemListingState.ItemListingType = VaultItemListingState.ItemListingType.Login,
        viewState: VaultItemListingState.ViewState = VaultItemListingState.ViewState.Loading,
    ): VaultItemListingState =
        VaultItemListingState(
            itemListingType = itemListingType,
            viewState = viewState,
        )
}
