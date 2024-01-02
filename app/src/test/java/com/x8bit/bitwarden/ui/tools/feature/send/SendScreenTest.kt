package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class SendScreenTest : BaseComposeTest() {

    private var onNavigateToNewSendCalled = false

    private val intentHandler = mockk<IntentHandler>()
    private val mutableEventFlow = bufferedMutableSharedFlow<SendEvent>()
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
                onNavigateAddSend = { onNavigateToNewSendCalled = true },
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `on overflow item click should display menu`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .isDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .isDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "About Send")
            .filterToOne(hasAnyAncestor(isPopup()))
            .isDisplayed()
    }

    @Test
    fun `on sync click should send SyncClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Sync")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.SyncClick)
        }
    }

    @Test
    fun `on lock click should send LockClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Lock")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.LockClick)
        }
    }

    @Test
    fun `on about send click should send AboutSendClick`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "About Send")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.AboutSendClick)
        }
    }

    @Test
    fun `fab should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Loading)
        }
        composeTestRule.onNodeWithContentDescription("Add item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule.onNodeWithContentDescription("Add item").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNodeWithContentDescription("Add item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Content)
        }
        composeTestRule.onNodeWithContentDescription("Add item").assertIsDisplayed()
    }

    @Test
    fun `on add item FAB click should send AddItemClick`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule
            .onNodeWithContentDescription("Add item")
            .performClick()
        verify { viewModel.trySendAction(SendAction.AddSendClick) }
    }

    @Test
    fun `on add item click should send AddItemClick`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
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
    fun `on NavigateToNewSend should call onNavigateToNewSend`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateNewSend)
        assert(onNavigateToNewSendCalled)
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Loading)
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Content)
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNodeWithText("Fail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun `on try again click should send send RefreshClick`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }

        composeTestRule.onNodeWithText("Try again").performClick()

        verify {
            viewModel.trySendAction(SendAction.RefreshClick)
        }
    }
}

private val DEFAULT_STATE: SendState = SendState(
    viewState = SendState.ViewState.Loading,
)
