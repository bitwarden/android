package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderDisplayItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FoldersScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToEditFolderScreenId: String? = null
    private var onNavigateToAddFolderScreenCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<FoldersEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<FoldersViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            FoldersScreen(
                viewModel = viewModel,
                onNavigateToEditFolderScreen = { onNavigateToEditFolderScreenId = it },
                onNavigateToAddFolderScreen = { onNavigateToAddFolderScreenCalled = true },
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(FoldersEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToAddFolderScreen should call onNavigateToAddFolderScreen`() {
        mutableEventFlow.tryEmit(FoldersEvent.NavigateToAddFolderScreen)
        assertTrue(onNavigateToAddFolderScreenCalled)
    }

    @Test
    fun `NavigateToEditFolderScreen should call onNavigateToEditFolderScreen`() {
        val tesId = "TestId"

        mutableEventFlow.tryEmit(FoldersEvent.NavigateToEditFolderScreen(tesId))
        assertEquals(tesId, onNavigateToEditFolderScreenId)
    }

    @Test
    fun `close button click should send CloseButtonClick`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(FoldersAction.CloseButtonClick) }
    }

    @Test
    fun `add folder button click should send AddFolderButtonClick`() {
        composeTestRule.onNodeWithContentDescription("Add Item").performClick()
        verify {
            viewModel.trySendAction(FoldersAction.AddFolderButtonClick)
        }
    }

    @Test
    fun `error text should display according to state`() {
        val message = "An error has occurred"

        mutableStateFlow.update { DEFAULT_LOADED_STATE }

        composeTestRule
            .onNodeWithText(message)
            .assertIsNotDisplayed()

        mutableStateFlow.update {
            FoldersState(
                viewState = FoldersState.ViewState.Error(
                    message = "An error has occurred".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(message)
            .assertIsDisplayed()
    }

    @Test
    fun `folders should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(
                viewState = FoldersState.ViewState.Content(emptyList()),
            )
        }

        composeTestRule
            .onNodeWithText("There are no folders to list.")
            .isNotDisplayed()

        mutableStateFlow.update { DEFAULT_LOADED_STATE }
        composeTestRule
            .onNodeWithText(text = "Test Folder 1")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(text = "Test Folder 2")
            .assertIsDisplayed()

        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithText(text = "Test Folder 1")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText(text = "Test Folder 2")
            .assertDoesNotExist()
    }

    @Test
    fun `clicking on a folder should send FolderClick action`() {
        mutableStateFlow.update { DEFAULT_LOADED_STATE }

        composeTestRule
            .onNodeWithText(text = "Test Folder 1")
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(FoldersAction.FolderClick("Id"))
        }
    }
}

private val DEFAULT_STATE =
    FoldersState(viewState = FoldersState.ViewState.Loading)

private val DEFAULT_LOADED_STATE =
    FoldersState(
        viewState = FoldersState.ViewState.Content(
            folderList = listOf(
                FolderDisplayItem(
                    id = "Id",
                    name = "Test Folder 1",
                ),
                FolderDisplayItem(
                    id = "Id 2",
                    name = "Test Folder 2",
                ),
            ),
        ),
    )
