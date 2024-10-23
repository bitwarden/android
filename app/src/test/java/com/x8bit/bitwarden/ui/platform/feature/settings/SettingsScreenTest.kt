package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenTest : BaseComposeTest() {

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<SettingsEvent>()
    private val viewModel = mockk<SettingsViewModel> {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Test
    fun `on about row click should emit SettingsClick`() {

        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT)) } just runs
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
        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
        } just runs
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
        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE))
        } just runs
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

        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL))
        } just runs
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
        composeTestRule.onNodeWithText("Autofill").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL)) }
    }

    @Test
    fun `on other row click should emit SettingsClick`() {

        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER)) } just runs
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

        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) } just runs
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAbout)
        assertTrue(haveCalledNavigateToAbout)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAccountSecurity`() {
        var haveCalledNavigateToAccountSecurity = false
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAccountSecurity)
        assertTrue(haveCalledNavigateToAccountSecurity)
    }

    @Test
    fun `on NavigateAccountSecurity should call NavigateAppearance`() {
        var haveCalledNavigateToAppearance = false
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAppearance)
        assertTrue(haveCalledNavigateToAppearance)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAutoFill`() {
        var haveCalledNavigateToAutoFill = false
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAutoFill)
        assertTrue(haveCalledNavigateToAutoFill)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToOther`() {
        var haveCalledNavigateToOther = false
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateOther)
        assertTrue(haveCalledNavigateToOther)
    }

    @Test
    fun `on NavigateAccountSecurity should call NavigateVault`() {
        var haveCalledNavigateToVault = false
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
        mutableEventFlow.tryEmit(SettingsEvent.NavigateVault)
        assertTrue(haveCalledNavigateToVault)
    }

    @Test
    fun `on NavigateAccountSecurityShortcut should call onNavigateToAccountSecurity`() {
        var haveCalledNavigateToAccountSecurity = false
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { },
                onNavigateToAccountSecurity = { haveCalledNavigateToAccountSecurity = true },
                onNavigateToAppearance = { },
                onNavigateToAutoFill = { },
                onNavigateToOther = { },
                onNavigateToVault = {
                },
            )
        }
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAccountSecurityShortcut)
        assertTrue(haveCalledNavigateToAccountSecurity)
    }

    @Test
    fun `Settings screen should show correct number of notification badges based on state`() {
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = {},
                onNavigateToAppearance = {},
                onNavigateToAutoFill = {},
                onNavigateToOther = {},
                onNavigateToVault = {},
                onNavigateToAccountSecurity = {},
            )
        }

        composeTestRule
            .onNodeWithText(text = "1", useUnmergedTree = true)
            .assertDoesNotExist()

        mutableStateFlow.update { it.copy(autoFillCount = 1) }

        composeTestRule
            .onNodeWithText(text = "1", useUnmergedTree = true)
            .assertExists()

        mutableStateFlow.update { it.copy(securityCount = 1) }

        composeTestRule
            .onAllNodesWithText(text = "1", useUnmergedTree = true)[0]
            .assertExists()

        composeTestRule
            .onAllNodesWithText(text = "1", useUnmergedTree = true)[1]
            .assertExists()

        mutableStateFlow.update { it.copy(vaultCount = 1) }

        composeTestRule
            .onAllNodesWithText(text = "1", useUnmergedTree = true)[0]
            .assertExists()

        composeTestRule
            .onAllNodesWithText(text = "1", useUnmergedTree = true)[1]
            .assertExists()

        composeTestRule
            .onAllNodesWithText(text = "1", useUnmergedTree = true)[2]
            .assertExists()
    }
}

private val DEFAULT_STATE = SettingsState(
    securityCount = 0,
    autoFillCount = 0,
    vaultCount = 0,
)
