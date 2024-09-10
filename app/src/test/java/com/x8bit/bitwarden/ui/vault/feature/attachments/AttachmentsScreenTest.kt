package com.x8bit.bitwarden.ui.vault.feature.attachments

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.isProgressBar
import com.x8bit.bitwarden.ui.util.onNodeWithContentDescriptionAfterScroll
import com.x8bit.bitwarden.ui.util.onNodeWithTextAfterScroll
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

class AttachmentsScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<AttachmentsEvent>()
    private val viewModel: AttachmentsViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }
    private val intentManager: IntentManager = mockk(relaxed = true)

    @Before
    fun setup() {
        composeTestRule.setContent {
            AttachmentsScreen(
                viewModel = viewModel,
                intentManager = intentManager,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AttachmentsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AttachmentsAction.BackClick)
        }
    }

    @Test
    fun `on save click should send SaveClick`() {
        composeTestRule.onNodeWithText("Save").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AttachmentsAction.SaveClick)
        }
    }

    @Test
    fun `on choose file click should send ChooseFileClick`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITHOUT_ATTACHMENTS)
        }

        composeTestRule.onNodeWithTextAfterScroll("Choose file").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AttachmentsAction.ChooseFileClick)
        }
    }

    @Test
    fun `progressbar should be displayed according to state`() {
        mutableStateFlow.update { it.copy(viewState = AttachmentsState.ViewState.Loading) }
        // There are 2 because of the pull-to-refresh
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(2)

        mutableStateFlow.update {
            it.copy(viewState = AttachmentsState.ViewState.Error("Fail".asText()))
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITHOUT_ATTACHMENTS)
        }
        // Only pull-to-refresh remains
        composeTestRule.onAllNodes(isProgressBar).assertCountEquals(1)
    }

    @Test
    fun `error should be displayed according to state`() {
        val errorMessage = "Fail"
        mutableStateFlow.update {
            it.copy(viewState = AttachmentsState.ViewState.Error(errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

        mutableStateFlow.update { it.copy(viewState = AttachmentsState.ViewState.Loading) }
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITHOUT_ATTACHMENTS)
        }
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()
    }

    @Test
    fun `content with no items should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITHOUT_ATTACHMENTS)
        }
        composeTestRule
            .onNodeWithTextAfterScroll("There are no attachments.")
            .assertIsDisplayed()
    }

    @Test
    fun `content with items should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        }
        composeTestRule
            .onNodeWithTextAfterScroll("cool_file.png")
            .assertIsDisplayed()
    }

    @Test
    fun `on delete click should display confirmation dialog`() {
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        }

        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Delete")
            .performClick()

        // Title
        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(!hasClickAction())
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        // Description
        composeTestRule
            .onAllNodesWithText("Do you really want to delete? This cannot be undone.")
            .filterToOne(!hasClickAction())
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        // Cancel Button
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasClickAction())
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        // Delete Button
        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasClickAction())
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on confirm delete click should send DeleteClick`() {
        val cipherId = "cipherId-1234"
        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_CONTENT_WITH_ATTACHMENTS)
        }

        composeTestRule
            .onNodeWithContentDescriptionAfterScroll("Delete")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasClickAction())
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(AttachmentsAction.DeleteClick(cipherId))
        }
    }

    @Test
    fun `error dialog should be displayed according to state`() {
        val errorMessage = "Fail"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = AttachmentsState.DialogState.Error(
                    title = null,
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "deleting"
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialogState = AttachmentsState.DialogState.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText(loadingMessage)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }
}

private val DEFAULT_STATE: AttachmentsState = AttachmentsState(
    cipherId = "cipherId-1234",
    viewState = AttachmentsState.ViewState.Loading,
    dialogState = null,
    isPremiumUser = false,
)

private val DEFAULT_CONTENT_WITHOUT_ATTACHMENTS: AttachmentsState.ViewState.Content =
    AttachmentsState.ViewState.Content(
        originalCipher = createMockCipherView(number = 1),
        attachments = emptyList(),
        newAttachment = null,
    )

private val DEFAULT_CONTENT_WITH_ATTACHMENTS: AttachmentsState.ViewState.Content =
    AttachmentsState.ViewState.Content(
        originalCipher = createMockCipherView(number = 1),
        attachments = listOf(
            AttachmentsState.AttachmentItem(
                id = "cipherId-1234",
                title = "cool_file.png",
                displaySize = "10 MB",
            ),
        ),
        newAttachment = null,
    )
