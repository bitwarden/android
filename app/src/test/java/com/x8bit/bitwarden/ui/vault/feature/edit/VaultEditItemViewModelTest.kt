package com.x8bit.bitwarden.ui.vault.feature.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultEditItemViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val differentVaultItemId = "something_different"
        val state = DEFAULT_STATE.copy(vaultItemId = differentVaultItemId)
        val viewModel = createViewModel(state = state)

        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultEditItemAction.CloseClick)
            assertEquals(VaultEditItemEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on SaveClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultEditItemAction.SaveClick)
            assertEquals(VaultEditItemEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    private fun createViewModel(
        state: VaultEditItemState? = DEFAULT_STATE,
        vaultItemId: String = VAULT_ITEM_ID,
    ): VaultEditItemViewModel = VaultEditItemViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
            set("vault_edit_item_id", vaultItemId)
        },
    )
}

private const val VAULT_ITEM_ID: String = "vault_item_id"

private val DEFAULT_STATE: VaultEditItemState = VaultEditItemState(
    vaultItemId = VAULT_ITEM_ID,
)
