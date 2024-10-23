package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockAutoFillScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<BlockAutoFillEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<BlockAutoFillViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            BlockAutoFillScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(BlockAutoFillAction.BackClick) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(BlockAutoFillEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `Screen should display empty state view when in ViewState Empty`() {
        composeTestRule
            .onNodeWithText("Autofill will not be offered for these URIs.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("New blocked URI")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `Screen should display content state view when in ViewState Content`() {
        mutableStateFlow.value = BlockAutoFillState(
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1", "uri2")),
        )

        listOf("uri1", "uri2").forEach { uri ->
            composeTestRule
                .onNodeWithText(uri)
                .assertIsDisplayed()
        }
    }

    @Test
    fun `empty state should display 'New blocked URI' button`() {
        mutableStateFlow.value = BlockAutoFillState(
            viewState = BlockAutoFillState.ViewState.Empty,
            dialog = null,
        )

        composeTestRule
            .onNodeWithText("New blocked URI")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `on add URI button click should send AddUriClick`() {
        composeTestRule
            .onNodeWithText("New blocked URI")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(BlockAutoFillAction.AddUriClick) }
    }

    @Test
    fun `on FAB button click should send AddUriClick`() {
        composeTestRule
            .onNodeWithContentDescription("Add Item")
            .performClick()

        verify { viewModel.trySendAction(BlockAutoFillAction.AddUriClick) }
    }

    @Test
    fun `on URI item click should send EditUriClick`() {
        mutableStateFlow.value = BlockAutoFillState(
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1")),
        )

        composeTestRule.onNodeWithText("uri1").performClick()

        verify { viewModel.trySendAction(BlockAutoFillAction.EditUriClick(uri = "uri1")) }
    }

    @Test
    fun `should show add URI dialog according to state`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = "",
                originalUri = null,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1")),
        )

        composeTestRule
            .onNodeWithText("New URI")
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `clicking a uri from the list should send EditUriClick action`() {
        val testUri = "http://test.com"

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = testUri,
                originalUri = testUri,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1")),
        )

        composeTestRule
            .onNodeWithText("New URI")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `should display error message in dialog when there is a error in the dialog state`() {
        val errorMessage = "Invalid URI"

        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = "invalid-uri",
                originalUri = null,
                errorMessage = errorMessage.asText(),
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf("uri1")),
        )

        composeTestRule
            .onNodeWithText(errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `clicking save in dialog should send SaveUri action`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = "http://newuri.com",
                originalUri = null,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf("existingUri")),
        )

        val newUri = "http://newuri.com"
        composeTestRule.onNodeWithText("Save").performClick()

        verify { viewModel.trySendAction(BlockAutoFillAction.SaveUri(newUri = newUri)) }
    }

    @Test
    fun `clicking cancel in dialog should send DismissDialog action`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = "http://uri.com",
                originalUri = null,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(emptyList()),
        )

        composeTestRule.onNodeWithText("Cancel").performClick()

        verify { viewModel.trySendAction(BlockAutoFillAction.DismissDialog) }
    }

    @Test
    fun `clicking remove in dialog should send RemoveUriClick action`() {
        composeTestRule.assertNoDialogExists()

        val uriToRemove = "http://uriToRemove.com"
        mutableStateFlow.value = BlockAutoFillState(
            dialog = BlockAutoFillState.DialogState.AddEdit(
                uri = uriToRemove,
                originalUri = uriToRemove,
                errorMessage = null,
            ),
            viewState = BlockAutoFillState.ViewState.Content(listOf(uriToRemove, "otherUri")),
        )

        composeTestRule.onNodeWithText("Remove").performClick()

        verify { viewModel.trySendAction(BlockAutoFillAction.RemoveUriClick(uri = uriToRemove)) }
    }
}

private val DEFAULT_STATE: BlockAutoFillState = BlockAutoFillState(
    viewState = BlockAutoFillState.ViewState.Empty,
)
