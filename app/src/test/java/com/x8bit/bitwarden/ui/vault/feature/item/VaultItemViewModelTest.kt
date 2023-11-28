package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultItemViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(vaultItemId = "something_different")
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultItemAction.CloseClick)
            assertEquals(VaultItemEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: VaultItemState? = DEFAULT_STATE,
        vaultItemId: String = VAULT_ITEM_ID,
    ): VaultItemViewModel = VaultItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("vault_item_id", vaultItemId)
        },
    )
}

private const val VAULT_ITEM_ID = "vault_item_id"

private val DEFAULT_STATE: VaultItemState = VaultItemState(
    vaultItemId = VAULT_ITEM_ID,
)
