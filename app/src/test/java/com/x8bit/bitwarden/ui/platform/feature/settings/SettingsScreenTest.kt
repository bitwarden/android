package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class SettingsScreenTest : BaseComposeTest() {

    @Test
    fun `on about row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.SettingsClick(Settings.ABOUT)) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("About").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT)) }
    }

    @Test
    fun `on account security row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every {
                trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
            } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Security").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY)) }
    }

    @Test
    fun `on appearance row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE)) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Language").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE)) }
    }

    @Test
    fun `on auto-fill row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL)) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Auto-fill").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL)) }
    }

    @Test
    fun `on other row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.SettingsClick(Settings.OTHER)) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Other").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER)) }
    }

    @Test
    fun `on vault row click should emit SettingsClick`() {
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) } returns Unit
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = { },
            )
        }
        composeTestRule.onNodeWithText("Vaults").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) }
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAccountSecurity`() {
        var haveCalledNavigateToAccountSecurity = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateAccountSecurity)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAccountSecurity = {
                    haveCalledNavigateToAccountSecurity = true
                },
            )
        }
        assertTrue(haveCalledNavigateToAccountSecurity)
    }
}
