package com.x8bit.bitwarden.ui.vault.feature.exportitems.reviewexport

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.cxf.manager.CredentialExchangeCompletionManager
import com.bitwarden.cxf.manager.model.ExportCredentialsResult
import com.bitwarden.cxf.model.ImportCredentialsRequestData
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewExportScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onSelectAnotherAccountCalled = false
    private val credentialExchangeCompletionManager = mockk<CredentialExchangeCompletionManager> {
        every { completeCredentialExport(any()) } just runs
    }
    private val mockStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mockEventFlow = bufferedMutableSharedFlow<ReviewExportEvent>()
    private val mockViewModel = mockk<ReviewExportViewModel> {
        every { stateFlow } returns mockStateFlow
        every { eventFlow } returns mockEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            credentialExchangeCompletionManager = credentialExchangeCompletionManager,
        ) {
            ReviewExportScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToAccountSelection = { onSelectAnotherAccountCalled = true },
                viewModel = mockViewModel,
            )
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mockEventFlow.tryEmit(ReviewExportEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `CompleteExport event should call credentialExchangeCompletionManager`() {
        val mockResult = mockk<ExportCredentialsResult>()
        mockEventFlow.tryEmit(ReviewExportEvent.CompleteExport(result = mockResult))
        verify {
            credentialExchangeCompletionManager.completeCredentialExport(mockResult)
        }
    }

    @Test
    fun `General dialog should display based on state`() {
        composeTestRule.assertNoDialogExists()

        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialog = ReviewExportState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    error = null,
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithText("title")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `General dialog dismiss should send DismissDialog action`() {
        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialog = ReviewExportState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    error = null,
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithText("Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            mockViewModel.trySendAction(ReviewExportAction.DismissDialog)
        }
    }

    @Test
    fun `Loading dialog should display based on state`() {
        composeTestRule.assertNoDialogExists()

        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                dialog = ReviewExportState.DialogState.Loading("message".asText()),
            ),
        )

        composeTestRule
            .onAllNodesWithText("message")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `Back click should send NavigateBackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            mockViewModel.trySendAction(ReviewExportAction.NavigateBackClick)
        }
    }

    @Test
    fun `EmptyContent should be displayed when no items to import`() {
        // Verify initial state is ReviewExportContent
        composeTestRule
            .onNodeWithText("No items available to import")
            .assertIsNotDisplayed()

        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ReviewExportState.ViewState.NoItems,
            ),
        )

        composeTestRule
            .onNodeWithText("No items available to import")
            .assertIsDisplayed()
    }

    @Test
    fun `SelectAnotherAccount click should send SelectAnotherAccountClick action`() {
        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ReviewExportState.ViewState.NoItems,
            ),
        )

        composeTestRule
            .onNodeWithText("Select a different account")
            .performClick()

        verify {
            mockViewModel.trySendAction(ReviewExportAction.SelectAnotherAccountClick)
        }
    }

    @Test
    fun `SelectAnotherAccount should not be visible when user do not have other accounts`() {
        mockStateFlow.tryEmit(
            DEFAULT_STATE.copy(
                viewState = ReviewExportState.ViewState.NoItems,
                hasOtherAccounts = false,
            ),
        )
        composeTestRule
            .onNodeWithText("Select a different account")
            .assertDoesNotExist()
    }
}

private val DEFAULT_STATE = ReviewExportState(
    viewState = ReviewExportState.ViewState.Content(
        itemTypeCounts = ReviewExportState.ItemTypeCounts(
            passwordCount = 1,
            passkeyCount = 1,
            identityCount = 1,
            cardCount = 1,
            secureNoteCount = 1,
        ),
    ),
    importCredentialsRequestData = ImportCredentialsRequestData(
        uri = Uri.EMPTY,
        requestJson = "",
    ),
    hasOtherAccounts = true,
    dialog = null,
)
