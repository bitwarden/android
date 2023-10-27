package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test

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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        composeTestRule.onNodeWithText("Account security").performClick()
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        composeTestRule.onNodeWithText("Appearance").performClick()
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        composeTestRule.onNodeWithText("Vault").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToAbout`() {
        var haveCalledNavigateToAbout = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateAbout)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = {
                    haveCalledNavigateToAbout = true
                },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        assertTrue(haveCalledNavigateToAbout)
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
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = {
                    haveCalledNavigateToAccountSecurity = true
                },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        assertTrue(haveCalledNavigateToAccountSecurity)
    }

    @Test
    fun `on NavigateAccountSecurity should call NavigateAppearance`() {
        var haveCalledNavigateToAppearance = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateAppearance)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { haveCalledNavigateToAppearance = true },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        assertTrue(haveCalledNavigateToAppearance)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAutoFill`() {
        var haveCalledNavigateToAutoFill = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateAutoFill)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = {
                    haveCalledNavigateToAutoFill = true
                },
                onNavigateToOther = { },
                onNavigateToVault = { },
            )
        }
        assertTrue(haveCalledNavigateToAutoFill)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToOther`() {
        var haveCalledNavigateToOther = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateOther)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = {
                    haveCalledNavigateToOther = true
                },
                onNavigateToVault = { },
            )
        }
        assertTrue(haveCalledNavigateToOther)
    }

    @Test
    fun `on NavigateAccountSecurity should call NavigateVault`() {
        var haveCalledNavigateToVault = false
        val viewModel = mockk<SettingsViewModel> {
            every { eventFlow } returns flowOf(SettingsEvent.NavigateVault)
        }
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = {
                    haveCalledNavigateToVault = true
                },
            )
        }
        assertTrue(haveCalledNavigateToVault)
    }
}
