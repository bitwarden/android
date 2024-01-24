package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PendingRequestsViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            viewState = PendingRequestsState.ViewState.Loading,
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(PendingRequestsAction.CloseClick)
            assertEquals(PendingRequestsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on DeclineAllRequestsClick should send ShowToast event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(PendingRequestsAction.DeclineAllRequestsClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                PendingRequestsEvent.ShowToast("Not yet implemented.".asText()),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: PendingRequestsState? = DEFAULT_STATE,
    ): PendingRequestsViewModel = PendingRequestsViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )

    companion object {
        val DEFAULT_STATE: PendingRequestsState = PendingRequestsState(
            viewState = PendingRequestsState.ViewState.Empty,
        )
    }
}
