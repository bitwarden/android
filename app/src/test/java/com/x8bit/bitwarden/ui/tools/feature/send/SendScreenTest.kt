package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SendScreenTest : BaseComposeTest() {

    private var onNavigateToNewSendCalled = false

    private val intentHandler = mockk<IntentHandler> {
        every { launchUri(any()) } just runs
    }
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
                onNavigateToAddSend = { onNavigateToNewSendCalled = true },
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `on NavigateToNewSend should call onNavigateToNewSend`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateNewSend)
        assertTrue(onNavigateToNewSendCalled)
    }

    @Test
    fun `on NavigateToAboutSend should call launchUri on intentHandler`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateToAboutSend)
        verify {
            intentHandler.launchUri("https://bitwarden.com/products/send".toUri())
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
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
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
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
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

    @Test
    fun `text type count should be updated according to state`() {
        val rowText = "Text"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, 1.toString())

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE.copy(textTypeCount = 3))
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, 3.toString())
    }

    @Test
    fun `text type row click should send TextTypeClick`() {
        val rowText = "Text"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.TextTypeClick)
        }
    }

    @Test
    fun `file type count should be updated according to state`() {
        val rowText = "File"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, 1.toString())

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE.copy(fileTypeCount = 3))
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .assertTextEquals(rowText, 3.toString())
    }

    @Test
    fun `file type row click should send FileTypeClick`() {
        val rowText = "File"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.FileTypeClick)
        }
    }

    @Test
    fun `on send item click should send SendClick`() {
        val rowText = "mockName-1"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(rowText))
        composeTestRule
            .onAllNodes(hasText(rowText))
            .filterToOne(hasClickAction())
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.SendClick(DEFAULT_SEND_ITEM))
        }
    }

    @Test
    fun `on send item overflow click should display dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNode(isDialog())
            .onChildren()
            .filterToOne(hasText(DEFAULT_SEND_ITEM.name))
            .assertIsDisplayed()
    }

    @Test
    fun `on send item overflow dialog edit click should send SendClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Edit")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.SendClick(DEFAULT_SEND_ITEM))
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on send item overflow dialog copy click should send CopyClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Copy link")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.CopyClick(DEFAULT_SEND_ITEM))
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on send item overflow dialog share link click should send ShareClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Share link")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.ShareClick(DEFAULT_SEND_ITEM))
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on send item overflow dialog cancel click should close the dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }
}

private val DEFAULT_STATE: SendState = SendState(
    viewState = SendState.ViewState.Loading,
)

private val DEFAULT_SEND_ITEM: SendState.ViewState.Content.SendItem =
    SendState.ViewState.Content.SendItem(
        id = "mockId-1",
        name = "mockName-1",
        deletionDate = "1",
        type = SendState.ViewState.Content.SendItem.Type.FILE,
        iconList = emptyList(),
        shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
    )

private val DEFAULT_CONTENT_VIEW_STATE: SendState.ViewState.Content = SendState.ViewState.Content(
    textTypeCount = 1,
    fileTypeCount = 1,
    sendItems = listOf(
        DEFAULT_SEND_ITEM,
        SendState.ViewState.Content.SendItem(
            id = "mockId-2",
            name = "mockName-2",
            deletionDate = "1",
            type = SendState.ViewState.Content.SendItem.Type.TEXT,
            iconList = emptyList(),
            shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
        ),
    ),
)
