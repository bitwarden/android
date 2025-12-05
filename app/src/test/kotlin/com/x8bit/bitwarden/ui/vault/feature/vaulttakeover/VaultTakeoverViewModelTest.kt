package com.x8bit.bitwarden.ui.vault.feature.vaulttakeover

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultTakeoverViewModelTest : BaseViewModelTest() {

    @Test
    fun `ContinueClicked sends NavigateToVault event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultTakeoverAction.ContinueClicked)
            assertEquals(VaultTakeoverEvent.NavigateToVault, awaitItem())
        }
    }

    @Test
    fun `DeclineAndLeaveClicked sends NavigateToLeaveOrganization event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultTakeoverAction.DeclineAndLeaveClicked)
            assertEquals(VaultTakeoverEvent.NavigateToLeaveOrganization, awaitItem())
        }
    }

    @Test
    fun `HelpLinkClicked sends LaunchUri event with help URL`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultTakeoverAction.HelpLinkClicked)
            val event = awaitItem()
            assert(event is VaultTakeoverEvent.LaunchUri)
            assertEquals("TODO_HELP_URL", (event as VaultTakeoverEvent.LaunchUri).uri)
        }
    }

    private fun createViewModel(): VaultTakeoverViewModel =
        VaultTakeoverViewModel()
}
