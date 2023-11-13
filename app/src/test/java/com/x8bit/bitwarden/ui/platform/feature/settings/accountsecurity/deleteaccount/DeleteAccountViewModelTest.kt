package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteAccountViewModelTest : BaseViewModelTest() {

    @Test
    fun `on CancelClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountAction.CancelClick)
            assertEquals(DeleteAccountEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountAction.CloseClick)
            assertEquals(DeleteAccountEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on DeleteAccountClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick)
            assertEquals(
                DeleteAccountEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): DeleteAccountViewModel = DeleteAccountViewModel()
}
