package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MigrateToMyItemsViewModelTest : BaseViewModelTest() {

    @Test
    fun `ContinueClicked sends NavigateToVault event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.ContinueClicked)
            assertEquals(MigrateToMyItemsEvent.NavigateToVault, awaitItem())
        }
    }

    @Test
    fun `DeclineAndLeaveClicked sends NavigateToLeaveOrganization event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.DeclineAndLeaveClicked)
            assertEquals(MigrateToMyItemsEvent.NavigateToLeaveOrganization, awaitItem())
        }
    }

    @Test
    fun `HelpLinkClicked sends LaunchUri event with help URL`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MigrateToMyItemsAction.HelpLinkClicked)
            val event = awaitItem()
            assert(event is MigrateToMyItemsEvent.LaunchUri)
            assertEquals("TODO_HELP_URL", (event as MigrateToMyItemsEvent.LaunchUri).uri)
        }
    }

    private fun createViewModel(): MigrateToMyItemsViewModel =
        MigrateToMyItemsViewModel()
}
