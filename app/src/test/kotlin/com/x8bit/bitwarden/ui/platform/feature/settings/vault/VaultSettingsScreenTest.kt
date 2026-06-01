package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.util.asText
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

class VaultSettingsScreenTest : BitwardenComposeTest() {

    private var onNavigateToImportLoginsCalled = false
    private var onNavigateToImportItemsCalled = false
    private var onNavigateBackCalled = false
    private var onNavigateToExportVaultCalled = false
    private var onNavigateToFoldersCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultSettingsEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    val viewModel = mockk<VaultSettingsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            VaultSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToExportVault = { onNavigateToExportVaultCalled = true },
                onNavigateToFolders = { onNavigateToFoldersCalled = true },
                onNavigateToImportLogins = { onNavigateToImportLoginsCalled = true },
                onNavigateToImportItems = { onNavigateToImportItemsCalled = true },
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        every { viewModel.trySendAction(VaultSettingsAction.BackClick) } just runs
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(VaultSettingsAction.BackClick) }
    }

    @Test
    fun `export vault click should send ExportVaultClick`() {
        composeTestRule.onNodeWithText("Export vault").performClick()
        verify {
            viewModel.trySendAction(VaultSettingsAction.ExportVaultClick)
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToExportVault should call onNavigateToExportVault`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateToExportVault)
        assertTrue(onNavigateToExportVaultCalled)
    }

    @Test
    fun `NavigateToFolders should call onNavigateToFolders`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateToFolders)
        assertTrue(onNavigateToFoldersCalled)
    }

    @Test
    fun `send action right when import items is clicked`() {
        composeTestRule.onNodeWithText("Import items").performClick()
        verify { viewModel.trySendAction(VaultSettingsAction.ImportItemsClick) }
    }

    @Test
    fun `NavigateToImportVault should invoke lambda`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateToImportVault)
        assertTrue(onNavigateToImportLoginsCalled)
    }

    @Test
    fun `when new show action card is true the import logins card should show`() {
        mutableStateFlow.update { it.copy(showImportActionCard = true) }
        composeTestRule
            .onNodeWithText("Import saved logins")
            .performScrollTo()
            .assertIsDisplayed()
        mutableStateFlow.update {
            it.copy(showImportActionCard = false)
        }
        composeTestRule
            .onNodeWithText("Import saved logins")
            .assertDoesNotExist()
    }

    @Test
    fun `when action card is visible clicking the close icon should send correct action`() {
        mutableStateFlow.update { it.copy(showImportActionCard = true) }
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardDismissClick)
        }
    }

    @Test
    fun `when action card is visible get started button should send correct action`() {
        mutableStateFlow.update { it.copy(showImportActionCard = true) }
        composeTestRule
            .onNodeWithText("Get started")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(VaultSettingsAction.ImportLoginsCardCtaClick)
        }
    }

    @Test
    fun `when ShowSnackbar is sent snackbar should be displayed`() {
        val data = BitwardenSnackbarData("message".asText())
        mutableEventFlow.tryEmit(VaultSettingsEvent.ShowSnackbar(data))
        composeTestRule.onNodeWithText("message").assertIsDisplayed()
    }

    @Test
    fun `when snackbar is displayed clicking on it should dismiss`() {
        val data = BitwardenSnackbarData("message".asText())
        mutableEventFlow.tryEmit(VaultSettingsEvent.ShowSnackbar(data))
        composeTestRule
            .onNodeWithText("message")
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText("message")
            .assertIsNotDisplayed()
    }
}

private val DEFAULT_STATE: VaultSettingsState = VaultSettingsState(
    showImportActionCard = false,
    showImportItemsChevron = true,
)
