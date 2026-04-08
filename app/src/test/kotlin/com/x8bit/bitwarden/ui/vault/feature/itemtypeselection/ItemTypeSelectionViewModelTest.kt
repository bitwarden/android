package com.x8bit.bitwarden.ui.vault.feature.itemtypeselection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemTypeSelectionViewModelTest : BaseViewModelTest() {

    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlag<Boolean>(FlagKey.NewItemTypes)
        } returns false
    }

    @Test
    fun `initial state should contain base item types when flag off`() {
        val viewModel = createViewModel()
        val cipherTypes = viewModel.stateFlow.value.itemTypes
            .map { it.cipherType }
        assertEquals(
            listOf(
                VaultItemCipherType.LOGIN,
                VaultItemCipherType.CARD,
                VaultItemCipherType.IDENTITY,
                VaultItemCipherType.SECURE_NOTE,
            ),
            cipherTypes,
        )
    }

    @Test
    fun `initial state should contain new item types when flag on`() {
        every {
            featureFlagManager.getFeatureFlag<Boolean>(FlagKey.NewItemTypes)
        } returns true
        val viewModel = createViewModel()
        val cipherTypes = viewModel.stateFlow.value.itemTypes
            .map { it.cipherType }
        assertTrue(
            cipherTypes.containsAll(
                listOf(
                    VaultItemCipherType.BANK_ACCOUNT,
                    VaultItemCipherType.DRIVERS_LICENSE,
                    VaultItemCipherType.PASSPORT,
                ),
            ),
        )
    }

    @Test
    fun `BackClick should send NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ItemTypeSelectionAction.BackClick)
            assertEquals(
                ItemTypeSelectionEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ItemTypeClick should send NavigateToAddItem event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(
                    ItemTypeSelectionAction.ItemTypeClick(
                        cipherType = VaultItemCipherType.LOGIN,
                    ),
                )
                assertEquals(
                    ItemTypeSelectionEvent.NavigateToAddItem(
                        cipherType = VaultItemCipherType.LOGIN,
                    ),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(): ItemTypeSelectionViewModel =
        ItemTypeSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            featureFlagManager = featureFlagManager,
        )
}
