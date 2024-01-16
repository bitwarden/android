package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.movetoorganization.util.createMockOrganizationList
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

    @Test
    fun `OrganizationSelect should update selected Organization`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = createVaultMoveToOrganizationState(
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "1",
                    ),
                ),
            ),
        )
        val action = VaultMoveToOrganizationAction.OrganizationSelect(
            VaultMoveToOrganizationState.ViewState.Content.Organization(
                id = "3",
                name = "Organization 3",
                collections = emptyList(),
            ),
        )
        val expectedState = createVaultMoveToOrganizationState(
            viewState = VaultMoveToOrganizationState.ViewState.Content(
                organizations = createMockOrganizationList(),
                selectedOrganizationId = "3",
            ),
        )

        viewModel.actionChannel.trySend(action)

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `CollectionSelect should update selected Collections`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = createVaultMoveToOrganizationState(
                    viewState = VaultMoveToOrganizationState.ViewState.Content(
                        organizations = createMockOrganizationList(),
                        selectedOrganizationId = "1",
                    ),
                ),
            ),
        )
        val selectCollection3Action = VaultMoveToOrganizationAction.CollectionSelect(
            VaultMoveToOrganizationState.ViewState.Content.Collection(
                id = "3",
                name = "Collection 3",
                isSelected = false,
            ),
        )
        val unselectCollection1Action = VaultMoveToOrganizationAction.CollectionSelect(
            VaultMoveToOrganizationState.ViewState.Content.Collection(
                id = "1",
                name = "Collection 1",
                isSelected = true,
            ),
        )
        val expectedState = createVaultMoveToOrganizationState(
            viewState = VaultMoveToOrganizationState.ViewState.Content(
                organizations = createMockOrganizationList()
                    .map { organization ->
                        organization.copy(
                            collections =
                                if (organization.id == "1") {
                                    organization.collections.map {
                                        it.copy(isSelected = it.id == "3")
                                    }
                                } else {
                                    organization.collections
                                },
                        )
                    },
                selectedOrganizationId = "1",
            ),
        )

        viewModel.actionChannel.trySend(selectCollection3Action)
        viewModel.actionChannel.trySend(unselectCollection1Action)

        assertEquals(
            expectedState,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveClick should show dialog, and remove it once an item is moved`() = runTest {
        val viewModel = createViewModel(
            savedStateHandle = createSavedStateHandleWithState(
                state = initialState,
            ),
        )
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
            viewModel.actionChannel.trySend(VaultMoveToOrganizationAction.MoveClick)
            assertEquals(
                initialState.copy(
                    dialogState = VaultMoveToOrganizationState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                initialState,
                awaitItem(),
            )
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
        viewState: VaultMoveToOrganizationState.ViewState = VaultMoveToOrganizationState.ViewState.Empty,
        vaultItemId: String = "mockId",
        dialogState: VaultMoveToOrganizationState.DialogState? = null,
    ): VaultMoveToOrganizationState =
        VaultMoveToOrganizationState(
            vaultItemId = vaultItemId,
            viewState = viewState,
            dialogState = dialogState,
        )
}
