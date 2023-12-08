package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PasswordHistoryViewModelTest : BaseViewModelTest() {

    private val initialState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading)

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(initialState, awaitItem())
        }
    }

    @Test
    fun `CloseClick action should emit NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(PasswordHistoryAction.CloseClick)
            assertEquals(PasswordHistoryEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `PasswordCopyClick action should emit password copied ShowToast event`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(
                PasswordHistoryAction.PasswordCopyClick(
                    PasswordHistoryState.GeneratedPassword(password = "Password", date = "Date"),
                ),
            )
            assertEquals(PasswordHistoryEvent.ShowToast("Not yet implemented."), awaitItem())
        }
    }

    @Test
    fun `PasswordClearClick action should emit password history cleared ShowToast event`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                viewModel.actionChannel.trySend(PasswordHistoryAction.PasswordClearClick)
                assertEquals(
                    PasswordHistoryEvent.ShowToast("Not yet implemented."),
                    awaitItem(),
                )
            }
        }

    //region Helper Functions

    private fun createViewModel(): PasswordHistoryViewModel {
        return PasswordHistoryViewModel()
    }

    //endregion Helper Functions
}
