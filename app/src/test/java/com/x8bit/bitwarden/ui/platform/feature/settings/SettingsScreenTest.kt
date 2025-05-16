package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
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

class SettingsScreenTest : BitwardenComposeTest() {
    private var haveCalledNavigateToAbout = false
    private var haveCalledNavigateToAccountSecurity = false
    private var haveCalledNavigateToAppearance = false
    private var haveCalledNavigateToAutoFill = false
    private var haveCalledNavigateToOther = false
    private var haveCalledNavigateToVault = false
    private var haveCalledNavigateBack = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<SettingsEvent>()
    private val viewModel = mockk<SettingsViewModel> {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setup() {
        setContent {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToAbout = { haveCalledNavigateToAbout = true },
                onNavigateToAccountSecurity = { haveCalledNavigateToAccountSecurity = true },
                onNavigateToAppearance = { haveCalledNavigateToAppearance = true },
                onNavigateToAutoFill = { haveCalledNavigateToAutoFill = true },
                onNavigateToOther = { haveCalledNavigateToOther = true },
                onNavigateToVault = { haveCalledNavigateToVault = true },
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
    }

    @Test
    fun `on about row click should emit SettingsClick`() {
        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT)) } just runs
        composeTestRule.onNodeWithText("About").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT)) }
    }

    @Test
    fun `on account security row click should emit SettingsClick`() {
        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
        } just runs
        composeTestRule.onNodeWithText("Account security").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY)) }
    }

    @Test
    fun `on appearance row click should emit SettingsClick`() {
        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE))
        } just runs
        composeTestRule.onNodeWithText("Appearance").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE)) }
    }

    @Test
    fun `on auto-fill row click should emit SettingsClick`() {
        every {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL))
        } just runs
        composeTestRule.onNodeWithText("Autofill").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL)) }
    }

    @Test
    fun `on other row click should emit SettingsClick`() {
        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER)) } just runs
        composeTestRule.onNodeWithText("Other").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER)) }
    }

    @Test
    fun `on vault row click should emit SettingsClick`() {
        every { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) } just runs
        composeTestRule.onNodeWithText("Vault").performClick()
        verify { viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT)) }
    }

    @Test
    fun `on NavigateAbout should call onNavigateToAbout`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAbout)
        assertTrue(haveCalledNavigateToAbout)
    }

    @Test
    fun `on NavigateAccountSecurity should call onNavigateToAccountSecurity`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAccountSecurity)
        assertTrue(haveCalledNavigateToAccountSecurity)
    }

    @Test
    fun `on NavigateAppearance should call onNavigateToAppearance`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAppearance)
        assertTrue(haveCalledNavigateToAppearance)
    }

    @Test
    fun `on NavigateAutoFill should call onNavigateToAutoFill`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAutoFill)
        assertTrue(haveCalledNavigateToAutoFill)
    }

    @Test
    fun `on NavigateOther should call onNavigateToOther`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateOther)
        assertTrue(haveCalledNavigateToOther)
    }

    @Test
    fun `on NavigateVault should call onNavigateToVault`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateVault)
        assertTrue(haveCalledNavigateToVault)
    }

    @Test
    fun `on NavigateAccountSecurityShortcut should call onNavigateToAccountSecurity`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateAccountSecurityShortcut)
        assertTrue(haveCalledNavigateToAccountSecurity)
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(SettingsEvent.NavigateBack)
        assertTrue(haveCalledNavigateBack)
    }

    @Test
    fun `should display correct items according to state`() {
        mutableStateFlow.update { it.copy(isPreAuth = false) }
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Account security")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Autofill")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Vault")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Appearance")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Other")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "About")
            .assertExists()

        mutableStateFlow.update { it.copy(isPreAuth = true) }
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Account security")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Autofill")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Vault")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(text = "Appearance")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "Other")
            .assertExists()
        composeTestRule
            .onNodeWithText(text = "About")
            .assertExists()
    }

    @Test
    fun `Settings screen should show correct number of notification badges based on state`() {
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
    isPreAuth = false,
    securityCount = 0,
    autoFillCount = 0,
    vaultCount = 0,
)
