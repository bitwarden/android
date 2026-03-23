package com.x8bit.bitwarden.ui.vault.feature.attachments.preview

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
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

class PreviewAttachmentScreenTest : BitwardenComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<PreviewAttachmentEvent>()
    private val viewModel: PreviewAttachmentViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }
    private val intentManager: IntentManager = mockk(relaxed = true)

    @Before
    fun setup() {
        setContent(intentManager = intentManager) {
            PreviewAttachmentScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PreviewAttachmentEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `back button click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(PreviewAttachmentAction.BackClick)
        }
    }

    @Test
    fun `download button click should send DownloadClick`() {
        composeTestRule.onNodeWithContentDescription("Download").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
        }
    }

    @Test
    fun `file name should be displayed in top app bar`() {
        composeTestRule.onNodeWithText(DEFAULT_FILE_NAME).assertIsDisplayed()
    }

    @Test
    fun `progress bar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = PreviewAttachmentState.ViewState.Loading())
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = PreviewAttachmentState.ViewState.Error("Error".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error state should display error message`() {
        val errorMessage = "Preview not available"
        mutableStateFlow.update {
            it.copy(
                viewState = PreviewAttachmentState.ViewState.Error(errorMessage.asText()),
            )
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `error state should display download file button`() {
        mutableStateFlow.update {
            it.copy(
                viewState = PreviewAttachmentState.ViewState.Error("Error".asText()),
            )
        }
        composeTestRule.onNodeWithText("Download file").assertIsDisplayed()
    }

    @Test
    fun `download file button click in error state should send DownloadClick`() {
        mutableStateFlow.update {
            it.copy(
                viewState = PreviewAttachmentState.ViewState.Error("Error".asText()),
            )
        }
        composeTestRule.onNodeWithText("Download file").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(PreviewAttachmentAction.DownloadClick)
        }
    }

    @Test
    fun `error dialog should be displayed according to state`() {
        val errorMessage = "Something went wrong"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Error(
                    title = null,
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `dismiss error dialog should send DismissDialog`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Error(
                    title = null,
                    message = "Error".asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText("Okay")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(PreviewAttachmentAction.DismissDialog)
        }
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "Downloading…"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Loading(
                    message = loadingMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `ShowSnackbar event should display the snackbar message`() {
        val message = "Attachment saved successfully"
        mutableEventFlow.tryEmit(
            PreviewAttachmentEvent.ShowSnackbar(
                data = BitwardenSnackbarData(message = message.asText()),
            ),
        )
        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog with title should display title and message`() {
        val title = "An error has occurred"
        val errorMessage = "Something went wrong"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = PreviewAttachmentState.DialogState.Error(
                    title = title.asText(),
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(title)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `preview unavailable dialog should display title and message`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable)
        }

        composeTestRule
            .onNodeWithText("Preview unavailable")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Close")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `close button on preview unavailable dialog should send CloseClick`() {
        mutableStateFlow.update {
            it.copy(dialogState = PreviewAttachmentState.DialogState.PreviewUnavailable)
        }
        composeTestRule
            .onNodeWithText("Close")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(PreviewAttachmentAction.CloseClick)
        }
    }

    @Test
    fun `NavigateToSelectAttachmentSaveLocation event should launch file chooser`() {
        val fileName = "test.png"
        mutableEventFlow.tryEmit(
            PreviewAttachmentEvent.NavigateToSelectAttachmentSaveLocation(fileName = fileName),
        )
        verify(exactly = 1) {
            intentManager.createDocumentIntent(fileName)
        }
    }
}

private const val DEFAULT_FILE_NAME = "test.png"

private val DEFAULT_STATE = PreviewAttachmentState(
    cipherId = "mockCipherId",
    attachmentId = "mockAttachmentId",
    fileName = DEFAULT_FILE_NAME,
    isPreviewable = true,
    viewState = PreviewAttachmentState.ViewState.Loading(),
    dialogState = null,
)
