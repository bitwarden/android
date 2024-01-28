package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class FolderAddEditScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<FolderAddEditEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE_ADD)
    val viewModel = mockk<FolderAddEditViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            FolderAddEditScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(FolderAddEditEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking save button should send SaveClick action`() {
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        verify {
            viewModel.trySendAction(
                FolderAddEditAction.SaveClick,
            )
        }
    }

    @Test
    fun `overflow menu should only be displayed in edit mode`() {
        composeTestRule
            .onNodeWithContentDescription("More")
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE_EDIT.copy(
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = "TestName",
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription("More")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking overflow menu and delete, and cancel should dismiss the dialog`() {
        val deleteText = "Do you really want to delete? This cannot be undone."
        mutableStateFlow.update {
            DEFAULT_STATE_EDIT.copy(
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = "TestName",
                ),
            )
        }

        // Open the overflow menu
        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()
        // Click on the delete item in the dropdown
        composeTestRule
            .onAllNodesWithText("Delete")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        composeTestRule
            .onNodeWithText(deleteText)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cancel")
            .performClick()

        composeTestRule
            .onNodeWithText(deleteText)
            .assertIsNotDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `clicking overflow menu and delete, and delete confirmation again should send a DeleteClick Action`() {
        val deleteText = "Do you really want to delete? This cannot be undone."

        mutableStateFlow.update {
            DEFAULT_STATE_EDIT.copy(
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = "TestName",
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("More")
            .performClick()

        composeTestRule
            .onNodeWithText("Delete")
            .performClick()

        composeTestRule
            .onNodeWithText(deleteText)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Delete")
            .performClick()

        composeTestRule
            .onNodeWithText(deleteText)
            .assertIsNotDisplayed()

        verify {
            viewModel.trySendAction(
                FolderAddEditAction.DeleteClick,
            )
        }
    }

    @Test
    fun `error text should display according to state`() {
        val message = "An error has occurred"

        mutableStateFlow.update { DEFAULT_STATE_ADD }

        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE_ADD.copy(
                viewState = FolderAddEditState.ViewState.Error(message.asText()),
            )
        }

        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog should display according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = FolderAddEditState.DialogState.Error(
                    message = "Error Message".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("An error has occurred.")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Error Message")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `loading dialog should display according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = FolderAddEditState.DialogState.Loading(
                    label = "Loading".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Loading")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `content should be displayed according to the state`() {
        composeTestRule
            .onNodeWithText("TestName")
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE_EDIT.copy(
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = "TestName",
                ),
            )
        }

        composeTestRule
            .onNodeWithText("TestName")
            .assertIsDisplayed()
    }
}

private val DEFAULT_STATE_ADD = FolderAddEditState(
    folderAddEditType = FolderAddEditType.AddItem,
    viewState = FolderAddEditState.ViewState.Loading,
    dialog = null,
)
private val DEFAULT_STATE_EDIT = FolderAddEditState(
    folderAddEditType = FolderAddEditType.EditItem("1"),
    viewState = FolderAddEditState.ViewState.Loading,
    dialog = null,
)
