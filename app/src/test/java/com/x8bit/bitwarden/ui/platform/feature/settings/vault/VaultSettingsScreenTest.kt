package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultSettingsScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToExportVaultCalled = false
    private var onNavigateToFoldersCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultSettingsEvent>()
    private val mutableStateFlow = MutableStateFlow(
        VaultSettingsState(
            importUrl = "testUrl/#/tools/import",
        ),
    )
    private val intentManager: IntentManager = mockk(relaxed = true) {
        every { launchUri(any()) } just runs
    }

    val viewModel = mockk<VaultSettingsViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            VaultSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToExportVault = { onNavigateToExportVaultCalled = true },
                onNavigateToFolders = { onNavigateToFoldersCalled = true },
                intentManager = intentManager,
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
    fun `import items click should display dialog and confirming should send ImportItemsClick`() {
        composeTestRule.onNodeWithText("Import items").performClick()
        composeTestRule
            .onNodeWithText("Continue")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
        }
    }

    @Test
    fun `import items click should display dialog & canceling should not send ImportItemsClick`() {
        composeTestRule.onNodeWithText("Import items").performClick()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 0) {
            viewModel.trySendAction(VaultSettingsAction.ImportItemsClick)
        }
    }

    @Test
    fun `import items click should display dialog with importUrl`() {
        composeTestRule.onNodeWithText("Import items").performClick()
        composeTestRule
            .onNodeWithText(mutableStateFlow.value.importUrl, substring = true)
            .assertIsDisplayed()
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
    fun `on NavigateToImportVault should invoke IntentManager`() {
        val testUrl = "testUrl"
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateToImportVault(testUrl))
        verify {
            intentManager.launchUri(testUrl.toUri())
        }
    }
}
