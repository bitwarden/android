package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
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

class AccountSecurityScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToDeleteAccountCalled = false

    private val intentHandler = mockk<IntentHandler> {
        every { launchUri(any()) } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<AccountSecurityEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AccountSecurityViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            AccountSecurityScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToDeleteAccount = { onNavigateToDeleteAccountCalled = true },
                viewModel = viewModel,
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `on Log out click should send LogoutClick`() {
        composeTestRule.onNodeWithText("Log out").performScrollTo().performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
    }

    @Test
    fun `on approve login requests toggle should send LoginRequestToggle`() {
        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LoginRequestToggle(true)) }
    }

    @Test
    fun `on approve login requests should be toggled on or off according to state`() {
        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .assertIsOff()
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = true) }
        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .assertIsOn()
    }

    @Test
    fun `on pending login requests click should send PendingLoginRequestsClick`() {
        composeTestRule
            .onNodeWithText("Pending login requests")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick) }
    }

    @Test
    fun `on unlock with biometrics toggle should send UnlockWithBiometricToggle`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggle(true)) }
    }

    @Test
    fun `on unlock with biometrics should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText("Unlock with Biometrics").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = true) }
        composeTestRule.onNodeWithText("Unlock with Biometrics").assertIsOn()
    }

    @Test
    fun `on unlock with pin toggle should send UnlockWithPinToggle`() {
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle(true)) }
    }

    @Test
    fun `on unlock with pin code should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText("Unlock with PIN code").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithPinEnabled = true) }
        composeTestRule.onNodeWithText("Unlock with PIN code").assertIsOn()
    }

    @Test
    fun `on session timeout click should send SessionTimeoutClick`() {
        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.SessionTimeoutClick) }
    }

    @Test
    fun `session timeout should be updated on or off according to state`() {
        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .assertTextEquals("Session timeout", "15 Minutes")
        mutableStateFlow.update { it.copy(sessionTimeout = "30 Minutes".asText()) }
        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .assertTextEquals("Session timeout", "30 Minutes")
    }

    @Test
    fun `on session timeout action click should send SessionTimeoutActionClick`() {
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.SessionTimeoutActionClick) }
    }

    @Test
    fun `session timeout action should be updated on or off according to state`() {
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .assertTextEquals("Session timeout action", "Lock")
        mutableStateFlow.update { it.copy(sessionTimeoutAction = SessionTimeoutAction.LOG_OUT) }
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .assertTextEquals("Session timeout action", "Log out")
    }

    @Test
    fun `session timeout action dialog should be displayed to state`() {
        composeTestRule.onNodeWithText("Vault timeout action").assertDoesNotExist()
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.SessionTimeoutAction) }
        composeTestRule.onNodeWithText("Vault timeout action").assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on two-step login click should display confirmation dialog and confirm click should send TwoStepLoginClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Two-step login")
            .performScrollTo()
            .performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify { viewModel.trySendAction(AccountSecurityAction.TwoStepLoginClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on change master password click should display confirmation dialog and confirm should send ChangeMasterPasswordClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Change master password")
            .performScrollTo()
            .performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify { viewModel.trySendAction(AccountSecurityAction.ChangeMasterPasswordClick) }
    }

    @Test
    fun `on Lock now click should send LockNowClick`() {
        composeTestRule.onNodeWithText("Lock now").performScrollTo().performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LockNowClick) }
    }

    @Test
    fun `on delete account click should send DeleteAccountClick`() {
        composeTestRule.onNodeWithText("Delete account").performScrollTo().performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.DeleteAccountClick) }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.BackClick) }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToDeleteAccount should call onNavigateToDeleteAccountCalled`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToDeleteAccount)
        assertTrue(onNavigateToDeleteAccountCalled)
    }

    @Test
    fun `confirm dialog be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.ConfirmLogout) }
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Are you sure you want to log out?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on confirm logout click should send ConfirmLogoutClick`() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.ConfirmLogout) }
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.ConfirmLogoutClick) }
    }

    @Test
    fun `on cancel click should send DismissDialog`() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.ConfirmLogout) }
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.DismissDialog) }
    }

    @Test
    fun `fingerprint phrase dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
        composeTestRule
            .onNodeWithText("Fingerprint phrase")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Learn more")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Close")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("fingerprint-placeholder")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on close click should send DismissDialog`() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
        composeTestRule
            .onNodeWithText("Close")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.DismissDialog) }
    }

    @Test
    fun `on learn more click should send FingerPrintLearnMoreClick`() {
        mutableStateFlow.update { it.copy(dialog = AccountSecurityDialog.FingerprintPhrase) }
        composeTestRule
            .onNodeWithText("Learn more")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.FingerPrintLearnMoreClick) }
    }

    @Test
    fun `on NavigateToFingerprintPhrase should call launchUri on intentHandler`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToFingerprintPhrase)
        verify {
            intentHandler.launchUri("http://bitwarden.com/help/fingerprint-phrase".toUri())
        }
    }

    companion object {
        private val DEFAULT_STATE = AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "fingerprint-placeholder".asText(),
            isApproveLoginRequestsEnabled = false,
            isUnlockWithBiometricsEnabled = false,
            isUnlockWithPinEnabled = false,
            sessionTimeout = "15 Minutes".asText(),
            sessionTimeoutAction = SessionTimeoutAction.LOCK,
        )
    }
}
