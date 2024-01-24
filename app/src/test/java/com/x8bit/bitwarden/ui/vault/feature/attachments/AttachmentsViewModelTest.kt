package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AttachmentsViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct when state is null`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when state is set`() = runTest {
        val initialState = DEFAULT_STATE.copy(cipherId = "123456789")
        val viewModel = createViewModel(initialState)
        assertEquals(initialState, viewModel.stateFlow.value)
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.BackClick)
            assertEquals(AttachmentsEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `SaveClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AttachmentsAction.SaveClick)
            assertEquals(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()), awaitItem())
        }
    }

    private fun createViewModel(
        initialState: AttachmentsState? = null,
    ): AttachmentsViewModel = AttachmentsViewModel(
        savedStateHandle = SavedStateHandle().apply {
            set("state", initialState)
            set("cipher_id", initialState?.cipherId ?: "cipherId-1234")
        },
    )
}

private val DEFAULT_STATE: AttachmentsState = AttachmentsState(
    cipherId = "cipherId-1234",
)
