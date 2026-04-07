package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportItemsScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToImportFromComputerCalled = false

    private val credentialExchangeImporter = mockk<CredentialExchangeImporter>()
    private val mockkStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mockEventFlow = bufferedMutableSharedFlow<ImportItemsEvent>()

    private val viewModel = mockk<ImportItemsViewModel> {
        every { eventFlow } returns mockEventFlow
        every { stateFlow } returns mockkStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            credentialExchangeImporter = credentialExchangeImporter,
        ) {
            ImportItemsScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToImportFromComputer = { onNavigateToImportFromComputerCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `initial state should be correct`() = runTest {
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )

        composeTestRule
            .onNodeWithText("Import from computer")
            .assertExists()

        composeTestRule
            .onNodeWithText("Import from another app")
            .assertExists()
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mockEventFlow.tryEmit(ImportItemsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `onBackClick should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(ImportItemsAction.BackClick)
        }
    }

    @Test
    fun `ImportFromComputer click should send NavigateToImportFromComputer action`() =
        runTest {
            composeTestRule
                .onNodeWithText("Import from computer")
                .performClick()

            verify(exactly = 1) {
                viewModel.trySendAction(ImportItemsAction.ImportFromComputerClick)
            }
        }

    @Test
    fun `NavigateToImportFromComputer should call onNavigateToImportFromComputer`() {
        mockEventFlow.tryEmit(ImportItemsEvent.NavigateToImportFromComputer)
        assertTrue(onNavigateToImportFromComputerCalled)
    }

    @Test
    fun `ImportFromAnotherApp click should send NavigateToImportFromAnotherApp action`() =
        runTest {
            composeTestRule
                .onNodeWithText("Import from another app")
                .performClick()
            verify(exactly = 1) {
                viewModel.trySendAction(ImportItemsAction.ImportFromAnotherAppClick)
            }
        }

    @Test
    fun `ShowRegisteredImportSources should call CredentialExchangeImporter`() = runTest {
        val importCredentialsSelectionResult = ImportCredentialsSelectionResult.Success(
            response = "mockResponse",
            callingAppInfo = mockk(relaxed = true),
        )
        coEvery {
            credentialExchangeImporter.importCredentials(listOf(""))
        } returns importCredentialsSelectionResult
        mockEventFlow.tryEmit(
            ImportItemsEvent.ShowRegisteredImportSources(
                listOf(""),
            ),
        )
        coVerify(exactly = 1) {
            credentialExchangeImporter.importCredentials(listOf(""))
            viewModel.trySendAction(
                ImportItemsAction.ImportCredentialSelectionReceive(
                    selectionResult = importCredentialsSelectionResult,
                ),
            )
        }
    }

    @Test
    fun `General dialog should display based on state`() = runTest {
        mockkStateFlow.tryEmit(ImportItemsState())

        composeTestRule
            .assertNoDialogExists()

        mockkStateFlow.tryEmit(
            ImportItemsState(
                dialog = ImportItemsState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    throwable = null,
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithText("title")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `General dialog dismiss should send DismissDialog`() = runTest {
        mockkStateFlow.tryEmit(
            ImportItemsState(
                dialog = ImportItemsState.DialogState.General(
                    title = "title".asText(),
                    message = "message".asText(),
                    throwable = null,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText("Okay")
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(ImportItemsAction.DismissDialog)
        }
    }

    @Test
    fun `BitwardenLoadingDialog should display based on state`() = runTest {
        mockkStateFlow.tryEmit(
            ImportItemsState(
                dialog = ImportItemsState.DialogState.Loading(message = "message".asText()),
            ),
        )

        composeTestRule
            .onNodeWithText("message")
            .assertIsDisplayed()
    }
}

private val DEFAULT_STATE: ImportItemsState = ImportItemsState()
