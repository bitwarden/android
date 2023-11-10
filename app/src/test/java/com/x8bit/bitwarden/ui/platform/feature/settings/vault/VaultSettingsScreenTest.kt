package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSettingsScreenTest : BaseComposeTest() {

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: VaultSettingsViewModel = mockk {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(VaultSettingsAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            VaultSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(VaultSettingsAction.BackClick) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToVault`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<VaultSettingsViewModel> {
            every { eventFlow } returns flowOf(VaultSettingsEvent.NavigateBack)
        }
        composeTestRule.setContent {
            VaultSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }
}
