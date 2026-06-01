package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.advanceTimeByAndRunCurrent
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.bitwarden.ui.util.isBottomSheet
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

class ImportLoginsScreenTest : BitwardenComposeTest() {
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
        setContent(
            intentManager = intentManager,
        ) {
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
        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = ImportLoginsState.DialogState.GetStarted,
            )
        }
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()

        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }
        composeTestRule
            .assertNoDialogExists()

        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = ImportLoginsState.DialogState.ImportLater,
            )
        }

        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when dialog state is GetStarted, GetStarted dialog is shown and sends correct actions when clicked`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = ImportLoginsState.DialogState.GetStarted,
            )
        }
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
        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = ImportLoginsState.DialogState.ImportLater,
            )
        }
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
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.InitialContent,
            )
        }
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.CloseClick)
    }

    @Test
    fun `Step one content is displayed when view state is ImportStepOne`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            )
        }
        composeTestRule
            .onNodeWithText("Step 1 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `while on step one correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            )
        }
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
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            )
        }
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToInitialContent)
    }

    @Test
    fun `Step two content is displayed when view state is ImportStepTwo`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            )
        }
        composeTestRule
            .onNodeWithText("Step 2 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `Step two content shows correct vault url when vault url is set`() {
        val url = "vault.bitwarden.com.testing"
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
                currentWebVaultUrl = url,
            )
        }
        composeTestRule
            .onNodeWithText("Step 2 of 3")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(url, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `while on step two correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            )
        }
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
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            )
        }
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToStepOne)
    }

    @Test
    fun `Step three content is displayed when view state is ImportStepThree`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            )
        }
        composeTestRule
            .onNodeWithText("Step 3 of 3")
            .assertIsDisplayed()
    }

    @Test
    fun `while on step three correct actions are sent when buttons are clicked`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            )
        }
        composeTestRule
            .onNodeWithText("Back")
            .performScrollTo()
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToStepTwo)
        composeTestRule
            .onNodeWithText("Done")
            .performClick()
        verifyActionSent(ImportLoginsAction.MoveToSyncInProgress)
    }

    @Test
    fun `while on step three system back returns to the previous content`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            )
        }
        backDispatcher?.onBackPressed()
        verifyActionSent(ImportLoginsAction.MoveToStepTwo)
    }

    @Test
    fun `Loading content is displayed when dialog state is syncing`() {
        composeTestRule.assertNoDialogExists()
        mutableImportLoginsStateFlow.update {
            it.copy(dialogState = ImportLoginsState.DialogState.Syncing)
        }
        composeTestRule
            .onNodeWithText(text = "Syncing logins…")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `Error dialog is displayed when dialog state is Error`() {
        mutableImportLoginsStateFlow.update {
            it.copy(
                dialogState = ImportLoginsState.DialogState.Error(),
            )
        }
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
            .onAllNodesWithText("Import logins later")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.FailedSyncAcknowledged)
    }

    @Test
    fun `Error dialog is displayed when dialog state is Error for no logins`() {
        mutableImportLoginsStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialogState = ImportLoginsState.DialogState.Error(
                    message = BitwardenString.no_logins_were_imported.asText(),
                ),
            ),
        )
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "No logins were imported",
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
            .onAllNodesWithText("Import logins later")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()
        verifyActionSent(ImportLoginsAction.FailedSyncAcknowledged)
    }

    @Test
    fun `Success bottom sheet is shown when state is updated`() {
        mutableImportLoginsStateFlow.update {
            it.copy(showBottomSheet = true)
        }
        composeTestRule
            .onNodeWithText("Import Successful!")
            .assertIsDisplayed()
    }

    @Test
    fun `SuccessfulSyncAcknowledged action is sent when bottom sheet CTA is clicked`() {
        mutableImportLoginsStateFlow.update {
            it.copy(showBottomSheet = true)
        }
        composeTestRule
            .onNodeWithText("Import Successful!")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Got it")
            .performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)

        dispatcher.advanceTimeByAndRunCurrent(1000L)

        verifyActionSent(ImportLoginsAction.SuccessfulSyncAcknowledged)
    }

    @Test
    fun `SuccessfulSyncAcknowledged action is sent when bottom sheet is closed`() {
        mutableImportLoginsStateFlow.update {
            it.copy(showBottomSheet = true)
        }
        composeTestRule
            .onNodeWithText("Import Successful!")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription("Close")
            .filterToOne(hasAnyAncestor(isBottomSheet))
            .assertIsDisplayed()
            .performClick()

        dispatcher.advanceTimeByAndRunCurrent(1000L)

        verifyActionSent(ImportLoginsAction.SuccessfulSyncAcknowledged)
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
    showBottomSheet = false,
    currentWebVaultUrl = "vault.bitwarden.com",
)
