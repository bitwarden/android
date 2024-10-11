package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportLoginsScreenTest : BaseComposeTest() {

    private var navigateBackCalled = false

    private val mutableImportLoginsStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableImportLoginsEventFlow = bufferedMutableSharedFlow<ImportLoginsEvent>()
    private val viewModel = mockk<ImportLoginsViewModel> {
        every { eventFlow } returns mutableImportLoginsEventFlow
        every { stateFlow } returns mutableImportLoginsStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            ImportLoginsScreen(
                onNavigateBack = { navigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `navigate back when event is NavigateBack`() {
        mutableImportLoginsEventFlow.tryEmit(ImportLoginsEvent.NavigateBack)

        assertTrue(navigateBackCalled)
    }

    @Test
    fun `when close icon clicked, CloseClick action is sent`() {
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performClick()

        verifyActionSent(ImportLoginsAction.CloseClick)
    }

    @Test
    fun `when get started clicked, GetStartedClick action is sent`() {
        composeTestRule
            .onNodeWithText("Get started")
            .performClick()

        verifyActionSent(ImportLoginsAction.GetStartedClick)
    }

    @Test
    fun `when import later clicked, ImportLaterClick action is sent`() {
        composeTestRule
            .onNodeWithText("Import logins later")
            .performClick()

        verifyActionSent(ImportLoginsAction.ImportLaterClick)
    }

    @Test
    fun `dialog content is shown when state updates and is hidden when null`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.GetStarted,
            ),
        )
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = null,
            ),
        )
        composeTestRule
            .assertNoDialogExists()

        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.ImportLater,
            ),
        )

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when dialog state is GetStarted, GetStarted dialog is shown and sends correct actions when clicked`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.GetStarted,
            ),
        )
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Do you have a computer available?", useUnmergedTree = true)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.ConfirmGetStarted)

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.DismissDialog)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when dialog state is ImportLater, ImportLater dialog is shown and sends correct actions when clicked`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.ImportLater,
            ),
        )
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Import logins later?", useUnmergedTree = true)
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Confirm")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.ConfirmImportLater)

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.DismissDialog)
    }

    private fun verifyActionSent(action: ImportLoginsAction) {
        verify { viewModel.trySendAction(action) }
    }
}

private val DEFAULT_STATE = ImportLoginsState(dialogState = null)
