package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemTypeSelectionViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should contain all eight cipher types in canonical order`() {
        val viewModel = createViewModel()
        val cipherTypes = viewModel.stateFlow.value.itemTypes.map { it.cipherType }
        assertEquals(
            listOf(
                VaultItemCipherType.LOGIN,
                VaultItemCipherType.CARD,
                VaultItemCipherType.IDENTITY,
                VaultItemCipherType.SECURE_NOTE,
                VaultItemCipherType.SSH_KEY,
                VaultItemCipherType.BANK_ACCOUNT,
                VaultItemCipherType.DRIVERS_LICENSE,
                VaultItemCipherType.PASSPORT,
            ),
            cipherTypes,
        )
    }

    @Test
    fun `BackClick action should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ItemTypeSelectionAction.BackClick)
            assertEquals(ItemTypeSelectionEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `ItemTypeClick action should send NavigateToAddItem event with the selected type`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    ItemTypeSelectionAction.ItemTypeClick(
                        cipherType = VaultItemCipherType.PASSPORT,
                    ),
                )
                assertEquals(
                    ItemTypeSelectionEvent.NavigateToAddItem(
                        cipherType = VaultItemCipherType.PASSPORT,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `state should be restored from SavedStateHandle when present`() {
        val savedState = ItemTypeSelectionState(itemTypes = persistentListOf())
        val viewModel = ItemTypeSelectionViewModel(
            savedStateHandle = SavedStateHandle(initialState = mapOf("state" to savedState)),
        )
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    private fun createViewModel(): ItemTypeSelectionViewModel =
        ItemTypeSelectionViewModel(savedStateHandle = SavedStateHandle())
}
