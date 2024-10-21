package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
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

class ImportLoginsScreenTest : BaseComposeTest() {
    private var navigateToImportLoginSuccessCalled = false
    private var navigateBackCalled = false

    private val mutableImportLoginsStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableImportLoginsEventFlow = bufferedMutableSharedFlow<ImportLoginsEvent>()
    private val viewModel = mockk<ImportLoginsViewModel> {
        every { eventFlow } returns mutableImportLoginsEventFlow
        every { stateFlow } returns mutableImportLoginsStateFlow
        every { trySendAction(any()) } just runs
    }
    private val intentManager = mockk<IntentManager> {
        every { startCustomTabsActivity(any()) } just runs
    }

    @Before
    fun setup() {
        setContentWithBackDispatcher {
            ImportLoginsScreen(
                onNavigateBack = { navigateBackCalled = true },
                onNavigateToImportSuccessScreen = { navigateToImportLoginSuccessCalled = true },
                viewModel = viewModel,
                intentManager = intentManager,
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

    @Test
    fun `OpenHelpLink event is used to open URI with intent manager`() {
        mutableImportLoginsEventFlow.tryEmit(ImportLoginsEvent.OpenHelpLink)
        verify {
            intentManager.startCustomTabsActivity("https://bitwarden.com/help/import-data/".toUri())
        }
    }

    @Test
    fun `while on initial content system back sends CloseClick action`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.InitialContent,
            ),
        )
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.CloseClick)
    }

    @Test
    fun `Step one content is displayed when view state is ImportStepOne`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            ),
        )
        composeTestRule
            .onNodeWithText("Step 1 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `while on step one correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            ),
        )
        composeTestRule
            .onNodeWithText("Back")
            .performScrollTo()
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToInitialContent)
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToStepTwo)
    }

    @Test
    fun `while on step one system back returns to the previous content`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            ),
        )
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToInitialContent)
    }

    @Test
    fun `Step two content is displayed when view state is ImportStepTwo`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            ),
        )
        composeTestRule
            .onNodeWithText("Step 2 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `while on step two correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            ),
        )
        composeTestRule
            .onNodeWithText("Back")
            .performScrollTo()
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToStepOne)
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToStepThree)
    }

    @Test
    fun `while on step two system back returns to the previous content`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            ),
        )
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToStepOne)
    }

    @Test
    fun `Step three content is displayed when view state is ImportStepThree`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            ),
        )
        composeTestRule
            .onNodeWithText("Step 3 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `while on step three correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            ),
        )
        composeTestRule
            .onNodeWithText("Back")
            .performScrollTo()
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToStepTwo)
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToSyncInProgress)
    }

    @Test
    fun `while on step three system back returns to the previous content`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            ),
        )
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToStepTwo)
    }

    @Test
    fun `NavigateToImportSuccess event causes correct lambda to invoke`() {
        mutableImportLoginsEventFlow.tryEmit(ImportLoginsEvent.NavigateToImportSuccess)
        assertTrue(navigateToImportLoginSuccessCalled)
    }

    @Test
    fun `Loading content is displayed when isVaultSyncing is true`() {
        mutableImportLoginsStateFlow.update {
            it.copy(isVaultSyncing = true)
        }
        composeTestRule.onRoot().printToLog("woo")
        composeTestRule
            .onNodeWithText(text = "Syncing logins...")
            .assertIsDisplayed()
    }

    @Test
    fun `Error dialog is displayed when dialog state is Error`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.Error,
            ),
        )
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "We were unable to process your request. Please try again or contact us.",
                useUnmergedTree = true,
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("Try again")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.RetryVaultSync)

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.FailSyncAcknowledged)
    }

    //region Helper methods

    private fun verifyActionSent(action: ImportLoginsAction) {
        verify { viewModel.trySendAction(action) }
    }

    //endregion Helper methods
}

private val DEFAULT_STATE = ImportLoginsState(
    dialogState = null,
    viewState = ImportLoginsState.ViewState.InitialContent,
    isVaultSyncing = false,
)
