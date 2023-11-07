package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class SendScreenTest : BaseComposeTest() {

    private var onNavigateToNewSendCalled = false
    private val mutableEventFlow = MutableSharedFlow<SendEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<SendViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            SendScreen(
                viewModel = viewModel,
                onNavigateNewSend = { onNavigateToNewSendCalled = true },
            )
        }
    }

    @Test
    fun `on add item FAB click should send AddItemClick`() {
        composeTestRule
            .onNodeWithContentDescription("Add item")
            .performClick()
        verify { viewModel.trySendAction(SendAction.AddSendClick) }
    }

    @Test
    fun `on add item click should send AddItemClick`() {
        composeTestRule
            .onNodeWithText("Add a Send")
            .performClick()
        verify { viewModel.trySendAction(SendAction.AddSendClick) }
    }

    @Test
    fun `on search click should send SearchClick`() {
        composeTestRule
            .onNodeWithContentDescription("Search Sends")
            .performClick()
        verify { viewModel.trySendAction(SendAction.SearchClick) }
    }

    @Test
    fun `on NavigateToNewSend should call onNavgiateToNewSend`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateNewSend)
        assert(onNavigateToNewSendCalled)
    }
}

private val DEFAULT_STATE = SendState.Empty
