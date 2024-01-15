package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultMoveToOrganizationViewModelTest : BaseViewModelTest() {

    private val initialState = createVaultMoveToOrganizationState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(
        state = initialState,
    )

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = null,
            ),
        )
        assertEquals(
            initialState.copy(viewState = VaultMoveToOrganizationState.ViewState.Loading),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val initState = createVaultMoveToOrganizationState()
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initState,
            ),
        )

        assertEquals(initState, viewModel.stateFlow.value)
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = initialSavedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(VaultMoveToOrganizationAction.BackClick)
            assertEquals(VaultMoveToOrganizationEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = initialSavedStateHandle,
    ): VaultMoveToOrganizationViewModel =
        VaultMoveToOrganizationViewModel(
            savedStateHandle = savedStateHandle,
        )

    private fun createSavedStateHandleWithState(
        state: VaultMoveToOrganizationState? = null,
        vaultItemId: String = "mockId",
    ) = SavedStateHandle().apply {
        set("state", state)
        set("vault_move_to_organization_id", vaultItemId)
    }

    @Suppress("MaxLineLength")
    private fun createVaultMoveToOrganizationState(
        viewState: VaultMoveToOrganizationState.ViewState = VaultMoveToOrganizationState.ViewState.Content,
        vaultItemId: String = "mockId",
    ): VaultMoveToOrganizationState =
        VaultMoveToOrganizationState(
            vaultItemId = vaultItemId,
            viewState = viewState,
        )
}
