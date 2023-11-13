package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            initials = "WB",
            avatarColorString = "00FF00",
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on AccountSwitchClick for the active account should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.AccountSwitchClick(
                    accountSummary = mockk {
                        every { status } returns AccountSummary.Status.ACTIVE
                    },
                ),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `on AccountSwitchClick for a locked account emit NavigateToVaultUnlockScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.AccountSwitchClick(
                    accountSummary = mockk {
                        every { status } returns AccountSummary.Status.LOCKED
                    },
                ),
            )
            assertEquals(VaultEvent.NavigateToVaultUnlockScreen, awaitItem())
        }
    }

    @Test
    fun `on AccountSwitchClick for an unlocked account emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(
                VaultAction.AccountSwitchClick(
                    accountSummary = mockk {
                        every { status } returns AccountSummary.Status.UNLOCKED
                    },
                ),
            )
            assertEquals(VaultEvent.ShowToast("Not yet implemented."), awaitItem())
        }
    }

    @Test
    fun `on AddAccountClick should emit NavigateToLoginScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.AddAccountClick)
            assertEquals(VaultEvent.NavigateToLoginScreen, awaitItem())
        }
    }

    @Test
    fun `AddItemClick should emit NavigateToAddItemScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.AddItemClick)
            assertEquals(VaultEvent.NavigateToAddItemScreen, awaitItem())
        }
    }

    @Test
    fun `CardGroupClick should emit NavigateToCardGroup`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.CardGroupClick)
            assertEquals(VaultEvent.NavigateToCardGroup, awaitItem())
        }
    }

    @Test
    fun `FolderClick should emit NavigateToFolder with correct folder ID`() = runTest {
        val viewModel = createViewModel()
        val folderId = "12345"
        val folder = mockk<VaultState.ViewState.FolderItem> {
            every { id } returns folderId
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.FolderClick(folder))
            assertEquals(VaultEvent.NavigateToFolder(folderId), awaitItem())
        }
    }

    @Test
    fun `IdentityGroupClick should emit NavigateToIdentityGroup`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.IdentityGroupClick)
            assertEquals(VaultEvent.NavigateToIdentityGroup, awaitItem())
        }
    }

    @Test
    fun `LoginGroupClick should emit NavigateToLoginGroup`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.LoginGroupClick)
            assertEquals(VaultEvent.NavigateToLoginGroup, awaitItem())
        }
    }

    @Test
    fun `SearchIconClick should emit NavigateToVaultSearchScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.SearchIconClick)
            assertEquals(VaultEvent.NavigateToVaultSearchScreen, awaitItem())
        }
    }

    @Test
    fun `SecureNoteGroupClick should emit NavigateToSecureNotesGroup`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.SecureNoteGroupClick)
            assertEquals(VaultEvent.NavigateToSecureNotesGroup, awaitItem())
        }
    }

    @Test
    fun `TrashClick should emit NavigateToTrash`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.TrashClick)
            assertEquals(VaultEvent.NavigateToTrash, awaitItem())
        }
    }

    @Test
    fun `VaultItemClick should emit NavigateToVaultItem with the correct item ID`() = runTest {
        val viewModel = createViewModel()
        val itemId = "54321"
        val item = mockk<VaultState.ViewState.VaultItem> {
            every { id } returns itemId
        }
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.VaultItemClick(item))
            assertEquals(VaultEvent.NavigateToVaultItem(itemId), awaitItem())
        }
    }

    private fun createViewModel(
        state: VaultState? = DEFAULT_STATE,
    ): VaultViewModel = VaultViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: VaultState = VaultState(
    avatarColorString = "FF0000FF",
    initials = "BW",
    accountSummaries = emptyList(),
    viewState = VaultState.ViewState.Loading,
)
