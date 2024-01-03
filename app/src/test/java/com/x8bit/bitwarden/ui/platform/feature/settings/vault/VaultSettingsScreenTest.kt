package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultSettingsScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToFoldersCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<VaultSettingsEvent>()
    private val mutableStateFlow = MutableStateFlow(Unit)
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
                onNavigateToFolders = { onNavigateToFoldersCalled = true },
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        every { viewModel.trySendAction(VaultSettingsAction.BackClick) } returns Unit
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
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToFolders should call onNavigateToFolders`() {
        mutableEventFlow.tryEmit(VaultSettingsEvent.NavigateToFolders)
        assertTrue(onNavigateToFoldersCalled)
    }
}
