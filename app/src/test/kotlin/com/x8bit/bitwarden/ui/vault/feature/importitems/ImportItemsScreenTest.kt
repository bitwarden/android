package com.x8bit.bitwarden.ui.vault.feature.importitems

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.cxf.importer.CredentialExchangeImporter
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.util.asText
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
    private var onNavigateToVaultCalled = false

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
        setContent {
            ImportItemsScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                credentialExchangeImporter = credentialExchangeImporter,
                onNavigateToVault = { onNavigateToVaultCalled = true },
            )
        }
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
    fun `NavigateToVault should call onNavigateToVault`() {
        mockEventFlow.tryEmit(ImportItemsEvent.NavigateToVault)
        assertTrue(onNavigateToVaultCalled)
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
    fun `onGetStartedClick should send GetStartedClick`() {
        mockkStateFlow.tryEmit(
            ImportItemsState(
                viewState = ImportItemsState.ViewState.NotStarted,
            ),
        )
        composeTestRule
            .onNodeWithText("Get started")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(ImportItemsAction.GetStartedClick)
        }
    }

    @Test
    fun `initial state should be NotStarted`() = runTest {
        mockkStateFlow.test {
            assertEquals(
                ImportItemsState(viewState = ImportItemsState.ViewState.NotStarted),
                awaitItem(),
            )
        }

        composeTestRule
            .onNodeWithText("Import saved items")
            .assertExists()

        composeTestRule
            .onNodeWithText(
                "Import your credentials, including passkeys, passwords, credit cards, and " +
                    "any personal identity information from another password manager.",
            )
            .assertExists()

        composeTestRule
            .onNodeWithText("Get started")
            .assertHasClickAction()
    }

    @Test
    fun `viewState should update to AwaitingSelection`() = runTest {
        mockkStateFlow.tryEmit(
            ImportItemsState(viewState = ImportItemsState.ViewState.AwaitingSelection),
        )

        composeTestRule
            .onNodeWithText("Select a credential manager to import items from.")
            .assertExists()
    }

    @Test
    fun `viewState should update to ImportingItems`() = runTest {
        mockkStateFlow.tryEmit(
            ImportItemsState(
                viewState = ImportItemsState.ViewState.ImportingItems(
                    message = "Importing items...".asText(),
                ),
            ),
        )
        composeTestRule
            .onNodeWithText("Importing items...")
            .assertExists()
    }

    @Test
    fun `viewState should update to Completed`() = runTest {
        mockkStateFlow.tryEmit(
            ImportItemsState(
                viewState = ImportItemsState.ViewState.Completed(
                    title = "title".asText(),
                    message = "message".asText(),
                    iconData = IconData.Local(
                        iconRes = BitwardenDrawable.icon,
                        contentDescription = "icon".asText(),
                    ),
                ),
            ),
        )

        composeTestRule
            .onNodeWithContentDescription("icon")
            .assertExists()

        composeTestRule
            .onNodeWithText("title")
            .assertExists()

        composeTestRule
            .onNodeWithText("message")
            .assertExists()

        composeTestRule
            .onNodeWithText("Return to your vault")
            .assertHasClickAction()
    }
}

private val DEFAULT_STATE: ImportItemsState = ImportItemsState(
    viewState = ImportItemsState.ViewState.NotStarted,
)
