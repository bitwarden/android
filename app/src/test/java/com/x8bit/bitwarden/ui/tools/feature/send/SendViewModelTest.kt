package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendViewModelTest : BaseViewModelTest() {

    private val vaultRepo: VaultRepository = mockk()

    @Test
    fun `initial state should be Empty`() {
        val viewModel = createViewModel()
        assertEquals(SendState.Empty, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should read from saved state when present`() {
        val savedState = mockk<SendState>()
        val viewModel = createViewModel(state = savedState)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `AboutSendClick should emit NavigateToAboutSend`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.AboutSendClick)
            assertEquals(SendEvent.NavigateToAboutSend, awaitItem())
        }
    }

    @Test
    fun `AddSendClick should emit NavigateNewSend`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.AddSendClick)
            assertEquals(SendEvent.NavigateNewSend, awaitItem())
        }
    }

    @Test
    fun `LockClick should lock the vault`() {
        val viewModel = createViewModel()
        every { vaultRepo.lockVaultForCurrentUser() } just runs

        viewModel.trySendAction(SendAction.LockClick)

        verify {
            vaultRepo.lockVaultForCurrentUser()
        }
    }

    @Test
    fun `SearchClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.SearchClick)
            assertEquals(SendEvent.ShowToast("Search Not Implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `SyncClick should call sync`() {
        val viewModel = createViewModel()
        every { vaultRepo.sync() } just runs

        viewModel.trySendAction(SendAction.SyncClick)

        verify {
            vaultRepo.sync()
        }
    }

    private fun createViewModel(
        state: SendState? = null,
        vaultRepository: VaultRepository = vaultRepo,
    ): SendViewModel = SendViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", state)
        },
        vaultRepo = vaultRepository,
    )
}
