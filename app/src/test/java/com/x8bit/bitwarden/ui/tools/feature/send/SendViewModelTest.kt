package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be Empty`() {
        val viewModel = SendViewModel(SavedStateHandle())
        assertEquals(SendState.Empty, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should read from saved state when present`() {
        val savedState = mockk<SendState>()
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = SendViewModel(handle)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `AddSendClick should emit ShowToast`() = runTest {
        val viewModel = SendViewModel(SavedStateHandle())
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.AddSendClick)
            assertEquals(SendEvent.ShowToast("New Send Not Implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `SearchClick should emit ShowToast`() = runTest {
        val viewModel = SendViewModel(SavedStateHandle())
        viewModel.eventFlow.test {
            viewModel.trySendAction(SendAction.SearchClick)
            assertEquals(SendEvent.ShowToast("Search Not Implemented".asText()), awaitItem())
        }
    }
}
