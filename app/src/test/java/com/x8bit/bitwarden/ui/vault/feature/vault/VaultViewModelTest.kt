package com.x8bit.bitwarden.ui.vault.feature.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultViewModelTest : BaseViewModelTest() {

    @Test
    fun `AddItemClick should navigate to the add item screen`() = runTest {
        val viewModel = VaultViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.AddItemClick)
            assertEquals(VaultEvent.NavigateToAddItemScreen, awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should navigate to the vault search screen`() = runTest {
        val viewModel = VaultViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.SearchIconClick)
            assertEquals(VaultEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }
}
