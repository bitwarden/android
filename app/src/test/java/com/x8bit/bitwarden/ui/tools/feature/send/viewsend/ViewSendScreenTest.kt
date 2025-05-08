package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ViewSendScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled: Boolean = false
    private var onNavigateToEditData: String? = null
    private val mutableEventFlow = bufferedMutableSharedFlow<ViewSendEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<ViewSendViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(action = any()) } just runs
    }

    @Before
    fun setup() {
        setContent {
            ViewSendScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToEditSend = { onNavigateToEditData = it },
            )
        }
    }

    @Test
    fun `on NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(ViewSendEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToEdit event should call onNavigateToEdit`() {
        val sendType = SendItemType.TEXT
        val sendId = "send_id"
        mutableEventFlow.tryEmit(ViewSendEvent.NavigateToEdit(sendType = sendType, sendId = sendId))
        assertEquals(sendId, onNavigateToEditData)
    }

    @Test
    fun `on close click should send CloseClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(ViewSendAction.CloseClick)
        }
    }

    @Test
    fun `on edit click should send EditClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Edit Send")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(ViewSendAction.EditClick)
        }
    }

    @Test
    fun `progress bar should be displayed based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = ViewSendState.ViewState.Loading) }
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        mutableStateFlow.update { it.copy(viewState = DEFAULT_STATE.viewState) }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)
    }

    @Test
    fun `error should be displayed based on ViewState`() {
        val errorMessage = "Fail!"
        mutableStateFlow.update {
            it.copy(viewState = ViewSendState.ViewState.Error(message = errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(text = errorMessage).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = DEFAULT_STATE.viewState) }
        composeTestRule.onNodeWithText(text = errorMessage).assertIsNotDisplayed()
    }
}

private val DEFAULT_STATE = ViewSendState(
    sendType = SendItemType.TEXT,
    sendId = "send_id",
    viewState = ViewSendState.ViewState.Content,
)
