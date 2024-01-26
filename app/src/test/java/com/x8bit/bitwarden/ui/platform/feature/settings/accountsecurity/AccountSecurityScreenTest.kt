package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.permissions.FakePermissionManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
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

@Suppress("LargeClass")
class AccountSecurityScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToDeleteAccountCalled = false
    private var onNavigateToPendingRequestsCalled = false

    private val intentManager = mockk<IntentManager> {
        every { launchUri(any()) } just runs
        every { startActivity(any()) } just runs
        every { startApplicationDetailsSettingsActivity() } just runs
    }
    private val permissionsManager = FakePermissionManager()
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
                onNavigateToPendingRequests = { onNavigateToPendingRequestsCalled = true },
                viewModel = viewModel,
                intentManager = intentManager,
                permissionsManager = permissionsManager,
            )
        }
    }

    @Test
    fun `on Log out click should send LogoutClick`() {
        composeTestRule.onNodeWithText("Log out").performScrollTo().performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on approve login requests toggle on should send PendingEnabled action and display dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Approve login requests")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "Use this device to approve login requests made from other devices",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.ApprovePasswordlessLoginsToggle.PendingEnabled,
            )
        }
    }

    @Test
    fun `on approve login requests toggle off should send Disabled action`() {
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = true) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on approve login requests confirm Yes should send Enabled action and hide dialog when permission already granted`() {
        permissionsManager.checkPermissionResult = true
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on approve login requests confirm Yes should send Enabled action and show permission dialog when permission not granted`() {
        permissionsManager.checkPermissionResult = false
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Receive push notifications for new login requests")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("No thanks")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Settings")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.ApprovePasswordlessLoginsToggle.Enabled,
            )
        }
    }

    @Test
    fun `on approve login requests confirm No should send Disabled action and hide dialog`() {
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.ApprovePasswordlessLoginsToggle.Disabled,
            )
        }
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
    fun `on push permission dialog No thanks should hide dialog`() {
        permissionsManager.checkPermissionResult = false
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("No thanks")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on push permission dialog Settings should hide dialog and send confirm action`() {
        permissionsManager.checkPermissionResult = false
        mutableStateFlow.update { it.copy(isApproveLoginRequestsEnabled = false) }

        composeTestRule
            .onNodeWithText("Use this device to approve login requests made from other devices")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Settings")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(AccountSecurityAction.PushNotificationConfirm)
        }
    }

    @Test
    fun `on NavigateToApplicationDataSettings should launch the correct intent`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToApplicationDataSettings)

        verify { intentManager.startApplicationDetailsSettingsActivity() }
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
    fun `on unlock with pin toggle when enabled should send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = true)
        }

        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle.Disabled) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on unlock with pin toggle when disabled should show the PIN input dialog and send UnlockWithPinToggle PendingEnabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }

        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Enter your PIN code.")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "Set your PIN code for unlocking Bitwarden. Your PIN settings will be reset if " +
                    "you ever fully log out of the application.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle.PendingEnabled) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Cancel click should clear the dialog and send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle.Disabled) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with empty pin should clear the dialog and send UnlockWithPinToggle Disabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle.Disabled) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with non-empty pin should show a confirmation dialog and send UnlockWithPinToggle PendingEnabled`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("1234")
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Unlock with PIN code")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "Do you want to require unlocking with your master password when the application " +
                    "is restarted?",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockWithPinToggle.PendingEnabled) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN confirmation dialog No click should send UnlockWithPinToggle Enabled and close the dialog`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("1234")
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle.Enabled(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = false,
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN confirmation dialog Yes click should send UnlockWithPinToggle Enabled and close the dialog`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performTextInput("1234")
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle.Enabled(
                    pin = "1234",
                    shouldRequireMasterPasswordOnRestart = true,
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on unlock with pin code should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText("Unlock with PIN code").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithPinEnabled = true) }
        composeTestRule.onNodeWithText("Unlock with PIN code").assertIsOn()
    }

    @Test
    fun `session timeout should be updated on or off according to state`() {
        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .assertTextEquals("Session timeout", "30 minutes")
        mutableStateFlow.update { it.copy(vaultTimeout = VaultTimeout.FourHours) }
        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .assertTextEquals("Session timeout", "4 hours")
    }

    @Test
    fun `on session timeout click should show a selection dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Immediately")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("1 minute")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("5 minutes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("15 minutes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("30 minutes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("1 hour")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("4 hours")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("On app restart")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Never")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Custom")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on session timeout selection dialog cancel click should close the dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout selection non-Never timeout type click should send VaultTimeoutTypeSelect and close the dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("4 hours")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.VaultTimeoutTypeSelect(VaultTimeout.Type.FOUR_HOURS),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout selection Never timeout type click should show a confirmation dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Never")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "Setting your lock options to “Never” keeps your vault available to anyone with " +
                    "access to your device. If you use this option, you should ensure that you " +
                    "keep your device properly protected.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on session timeout Never confirmation dialog Cancel click should close the dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Never")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout Never confirmation dialog Ok click should close the dialog and emit VaultTimeoutTypeSelect`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onAllNodesWithText("Session timeout")
            .filterToOne(hasClickAction())
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Never")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.VaultTimeoutTypeSelect(VaultTimeout.Type.NEVER),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `custom session timeout should update according to state`() {
        composeTestRule
            .onNodeWithText("Custom")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 0))
        }

        composeTestRule
            // Check for exact text to differentiate from the Custom label on the Vault Timeout
            // item above.
            .onNode(hasTextExactly("Custom", "00:00"))
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }

        composeTestRule
            .onNode(hasTextExactly("Custom", "02:03"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 1234))
        }

        composeTestRule
            .onNode(hasTextExactly("Custom", "20:34"))
            .assertIsDisplayed()
    }

    @Test
    fun `custom session timeout click should show a time-picker dialog`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }
        composeTestRule
            .onNode(hasTextExactly("Custom", "02:03"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Time")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `custom session timeout dialog Cancel click should dismiss the dialog`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }
        composeTestRule
            .onNode(hasTextExactly("Custom", "02:03"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `custom session timeout dialog Ok click should dismiss the dialog and send CustomVaultTimeoutSelect`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }
        composeTestRule
            .onNode(hasTextExactly("Custom", "02:03"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.CustomVaultTimeoutSelect(
                    VaultTimeout.Custom(vaultTimeoutInMinutes = 123),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on session timeout action click should show a selection dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Vault timeout action")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Lock")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout action dialog Lock click should close the dialog and send VaultTimeoutActionSelect`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Vault timeout action")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Lock")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.VaultTimeoutActionSelect(
                    VaultTimeoutAction.LOCK,
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout action dialog Logout click should open a confirmation dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                "Logging out will remove all access to your vault and requires online " +
                    "authentication after the timeout period. Are you sure you want to use this " +
                    "setting?",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout action Logout confirmation dialog cancel click should dismiss the dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.assertNoDialogExists()
        verify(exactly = 0) { viewModel.trySendAction(any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout action Logout confirmation dialog Yes click should dismiss the dialog and send VaultTimeoutActionSelect`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Warning")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.assertNoDialogExists()
        verify {
            viewModel.trySendAction(
                AccountSecurityAction.VaultTimeoutActionSelect(
                    VaultTimeoutAction.LOGOUT,
                ),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on session timeout action dialog cancel click should close the dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Vault timeout action")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        verify(exactly = 0) { viewModel.trySendAction(any()) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `session timeout action should be updated according to state`() {
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .assertTextEquals("Session timeout action", "Lock")
        mutableStateFlow.update { it.copy(vaultTimeoutAction = VaultTimeoutAction.LOGOUT) }
        composeTestRule
            .onNodeWithText("Session timeout action")
            .performScrollTo()
            .assertTextEquals("Session timeout action", "Log out")
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

    @Test
    fun `on NavigateToTwoStepLogin should call launchUri on intentManager`() {
        val uri = "testUri"
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToTwoStepLogin(uri))
        verify { intentManager.launchUri(uri.toUri()) }
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
    fun `on NavigateToChangeMasterPassword should call launchUri on intentManager`() {
        val uri = "testUri"
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToChangeMasterPassword(uri))
        verify { intentManager.launchUri(uri.toUri()) }
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
    fun `on NavigateToDeleteAccount should call onNavigateToDeleteAccount`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToDeleteAccount)
        assertTrue(onNavigateToDeleteAccountCalled)
    }

    @Test
    fun `on NavigateToPendingRequests should call onNavigateToPendingRequests`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToPendingRequests)
        assertTrue(onNavigateToPendingRequestsCalled)
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
    fun `on NavigateToFingerprintPhrase should call launchUri on intentManager`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToFingerprintPhrase)
        verify {
            intentManager.launchUri("http://bitwarden.com/help/fingerprint-phrase".toUri())
        }
    }

    companion object {
        private val DEFAULT_STATE = AccountSecurityState(
            dialog = null,
            fingerprintPhrase = "fingerprint-placeholder".asText(),
            isApproveLoginRequestsEnabled = false,
            isUnlockWithBiometricsEnabled = false,
            isUnlockWithPinEnabled = false,
            vaultTimeout = VaultTimeout.ThirtyMinutes,
            vaultTimeoutAction = VaultTimeoutAction.LOCK,
        )
    }
}
