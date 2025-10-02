package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendRoute
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
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

class ViewSendScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled: Boolean = false
    private var onNavigateToAddEditRoute: AddEditSendRoute? = null
    private val mutableEventFlow = bufferedMutableSharedFlow<ViewSendEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<ViewSendViewModel> {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(action = any()) } just runs
    }

    private var intentManager = mockk<IntentManager> {
        every { shareText(any()) } just runs
    }

    @Before
    fun setup() {
        setContent(
            intentManager = intentManager,
        ) {
            ViewSendScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToAddEditSend = { onNavigateToAddEditRoute = it },
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
        assertEquals(
            AddEditSendRoute(sendId = sendId, sendType = sendType, modeType = ModeType.EDIT),
            onNavigateToAddEditRoute,
        )
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
    fun `on ShareText event should call IntentManager ShareText`() {
        val text = "Shared Stuff"
        mutableEventFlow.tryEmit(ViewSendEvent.ShareText(text = text.asText()))
        verify(exactly = 1) {
            intentManager.shareText(text = text)
        }
    }

    @Test
    fun `on ShowSnackbar event should display the snackbar`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        mutableEventFlow.tryEmit(ViewSendEvent.ShowSnackbar(data = data))
        composeTestRule
            .onNodeWithText(text = message)
            .assertIsDisplayed()
    }

    @Test
    fun `on copy click should send CopyClick`() {
        composeTestRule
            .onNodeWithText(text = "Copy")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(ViewSendAction.CopyClick)
        }
    }

    @Test
    fun `on Delete button click should Display delete confirmation dialog`() {
        composeTestRule
            .onNodeWithText(text = "Delete Send")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Are you sure you want to delete this Send?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on delete confirmation dialog yes click should send DeleteClick`() {
        composeTestRule
            .onNodeWithText(text = "Delete Send")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ViewSendAction.DeleteClick)
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
    fun `on app bar title should updated based on state`() {
        mutableStateFlow.update { it.copy(sendType = SendItemType.TEXT) }
        composeTestRule.onNodeWithText(text = "View file Send").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "View text Send").assertIsDisplayed()

        mutableStateFlow.update { it.copy(sendType = SendItemType.FILE) }
        composeTestRule.onNodeWithText(text = "View file Send").assertIsDisplayed()
        composeTestRule.onNodeWithText(text = "View text Send").assertDoesNotExist()
    }

    @Test
    fun `progress bar should be displayed based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = ViewSendState.ViewState.Loading) }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = DEFAULT_STATE.viewState) }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
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

    @Test
    fun `file type content should be displayed based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = ViewSendState.ViewState.Loading) }
        composeTestRule.onNodeWithText(text = "share_link").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "text_to_share").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "send_name").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "deletion_date").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    sendType = ViewSendState.ViewState.Content.SendType.TextType(
                        textToShare = "text_to_share",
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "share_link")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "text_to_share")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "send_name")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "deletion_date")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `text type content should be displayed based on ViewState`() {
        mutableStateFlow.update { it.copy(viewState = ViewSendState.ViewState.Loading) }
        composeTestRule.onNodeWithText(text = "file_name").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "file_size").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "send_name").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "deletion_date").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                viewState = DEFAULT_CONTENT_VIEW_STATE.copy(
                    sendType = ViewSendState.ViewState.Content.SendType.FileType(
                        fileName = "file_name",
                        fileSize = "file_size",
                    ),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "share_link")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "file_name")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "file_size")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "send_name")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "deletion_date")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `additional options should reveal themselves when clicked`() {
        composeTestRule.onNodeWithText(text = "Maximum access count").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "Current access count: 1").assertDoesNotExist()
        composeTestRule.onNodeWithText(text = "Private notes").assertDoesNotExist()

        composeTestRule
            .onNodeWithText(text = "Additional options")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNodeWithText(text = "Maximum access count")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Current access count: 1")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Private notes")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `on copy notes click should send CopyNotesClick`() {
        composeTestRule
            .onNodeWithText(text = "Additional options")
            .performScrollTo()
            .performClick()

        // Overscroll to the delete button in order to avoid clicking the FAB
        composeTestRule.onNodeWithText(text = "Delete Send").performScrollTo()
        composeTestRule.onNodeWithContentDescription(label = "Copy note").performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ViewSendAction.CopyNotesClick)
        }
    }

    @Test
    fun `dialog should be displayed based on ViewState`() {
        composeTestRule.assertNoDialogExists()
        val errorMessage = "Fail!"
        mutableStateFlow.update {
            it.copy(
                dialogState = ViewSendState.DialogState.Error(
                    title = null,
                    message = errorMessage.asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        val loadingMessage = "Loading!"
        mutableStateFlow.update {
            it.copy(
                dialogState = ViewSendState.DialogState.Loading(
                    message = loadingMessage.asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = loadingMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.assertNoDialogExists()
    }
}

private val DEFAULT_CONTENT_VIEW_STATE = ViewSendState.ViewState.Content(
    sendType = ViewSendState.ViewState.Content.SendType.TextType(
        textToShare = "text_to_share",
    ),
    shareLink = "share_link",
    sendName = "send_name",
    deletionDate = "deletion_date",
    maxAccessCount = 1,
    currentAccessCount = 1,
    notes = "notes",
)

private val DEFAULT_STATE = ViewSendState(
    sendType = SendItemType.TEXT,
    sendId = "send_id",
    viewState = DEFAULT_CONTENT_VIEW_STATE,
    dialogState = null,
    baseWebSendUrl = "https://send.bitwarden.com/#",
)
