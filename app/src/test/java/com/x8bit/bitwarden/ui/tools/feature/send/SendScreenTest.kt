package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
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

@Suppress("LargeClass")
class SendScreenTest : BaseComposeTest() {

    private var onNavigateToNewSendCalled = false
    private var onNavigateToSendFilesListCalled = false
    private var onNavigateToSendTextListCalled = false
    private var onNavigateToSendSearchCalled = false
    private var onNavigateToEditSendId: String? = null

    private val intentManager = mockk<IntentManager> {
        every { launchUri(any()) } just runs
        every { shareText(any()) } just runs
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
                onNavigateToEditSend = { onNavigateToEditSendId = it },
                onNavigateToSendFilesList = { onNavigateToSendFilesListCalled = true },
                onNavigateToSendTextList = { onNavigateToSendTextListCalled = true },
                onNavigateToSearchSend = { onNavigateToSendSearchCalled = true },
                intentManager = intentManager,
            )
        }
    }

    @Test
    fun `on NavigateToNewSend should call onNavigateToNewSend`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateNewSend)
        assertTrue(onNavigateToNewSendCalled)
    }

    @Test
    fun `on NavigateToEditSend should call onNavigateToEditSend`() {
        val sendId = "sendId1234"
        mutableEventFlow.tryEmit(SendEvent.NavigateToEditSend(sendId))
        assertEquals(sendId, onNavigateToEditSendId)
    }

    @Test
    fun `on NavigateToFileSends should call onNavigateToSendFilesList`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateToFileSends)
        assertTrue(onNavigateToSendFilesListCalled)
    }

    @Test
    fun `on NavigateToTextSends should call onNavigateToSendTextList`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateToTextSends)
        assertTrue(onNavigateToSendTextListCalled)
    }

    @Test
    fun `on NavigateToSearch should call onNavigateToSendSearch`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateToSearch)
        assertTrue(onNavigateToSendSearchCalled)
    }

    @Test
    fun `on NavigateToAboutSend should call launchUri on intentManager`() {
        mutableEventFlow.tryEmit(SendEvent.NavigateToAboutSend)
        verify {
            intentManager.launchUri("https://bitwarden.com/products/send".toUri())
        }
    }

    @Test
    fun `on ShowShareSheet should call shareText on IntentManager`() {
        val text = "sharable stuff"
        mutableEventFlow.tryEmit(SendEvent.ShowShareSheet(text))
        verify {
            intentManager.shareText(text)
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
    fun `policy warning should update according to state`() {
        val policyText = "Due to an enterprise policy, you are only " +
            "able to delete an existing Send."
        composeTestRule
            .onNodeWithText(policyText)
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Empty,
                policyDisablesSend = true,
            )
        }

        composeTestRule
            .onNodeWithText(policyText)
            .assertIsDisplayed()
    }

    @Test
    fun `fab should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Loading)
        }
        composeTestRule.onNodeWithContentDescription("Add Item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule.onNodeWithContentDescription("Add Item").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }
        composeTestRule.onNodeWithContentDescription("Add Item").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        composeTestRule.onNodeWithContentDescription("Add Item").assertIsDisplayed()
    }

    @Test
    fun `on add item FAB click should send AddItemClick`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule
            .onNodeWithContentDescription("Add Item")
            .performClick()
        verify { viewModel.trySendAction(SendAction.AddSendClick) }
    }

    @Test
    fun `on add item click should send AddItemClick`() {
        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        composeTestRule
            .onNodeWithText("New send")
            .performScrollTo()
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
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Empty)
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        mutableStateFlow.update {
            it.copy(viewState = SendState.ViewState.Error("Fail".asText()))
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_VIEW_STATE)
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)
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
    fun `send item overflow button should update according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(DEFAULT_SEND_ITEM),
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                policyDisablesSend = true,
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Options")
            .assertDoesNotExist()
    }

    @Test
    fun `on send item overflow click should display dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText(DEFAULT_SEND_ITEM.name)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on send item overflow dialog edit click should send SendClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
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
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
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
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
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
    fun `on send item overflow dialog remove password click should send RemovePasswordClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Remove password")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.RemovePasswordClick(DEFAULT_SEND_ITEM))
        }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on send item overflow dialog delete click should show confirmation dialog`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Delete")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText("Are you sure you want to delete this Send?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on delete confirmation dialog yes click should send DeleteSendClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = SendState.ViewState.Content(
                    textTypeCount = 0,
                    fileTypeCount = 1,
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Delete")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(SendAction.DeleteSendClick(DEFAULT_SEND_ITEM))
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
                    sendItems = listOf(
                        DEFAULT_SEND_ITEM,
                        DEFAULT_SEND_ITEM.copy(id = "mockId-2"),
                    ),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()

        // We scroll to the last item but click the first one to avoid clicking the FAB by mistake
        composeTestRule
            .onAllNodesWithContentDescription("Options")
            .apply { onLast().performScrollTo() }
            .onFirst()
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `error dialog should be displayed according to state`() {
        val errorMessage = "Failure"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = SendState.DialogState.Error(
                    title = null,
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))

        composeTestRule
            .onNodeWithText("Ok")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(SendAction.DismissDialog)
        }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "syncing"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = SendState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }
}

private val DEFAULT_STATE: SendState = SendState(
    viewState = SendState.ViewState.Loading,
    dialogState = null,
    isPullToRefreshSettingEnabled = false,
    policyDisablesSend = false,
    isRefreshing = false,
)

private val DEFAULT_SEND_ITEM: SendState.ViewState.Content.SendItem =
    SendState.ViewState.Content.SendItem(
        id = "mockId-1",
        name = "mockName-1",
        deletionDate = "1",
        type = SendState.ViewState.Content.SendItem.Type.FILE,
        iconList = emptyList(),
        shareUrl = "www.test.com/#/send/mockAccessId-1/mockKey-1",
        hasPassword = true,
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
            hasPassword = true,
        ),
    ),
)
