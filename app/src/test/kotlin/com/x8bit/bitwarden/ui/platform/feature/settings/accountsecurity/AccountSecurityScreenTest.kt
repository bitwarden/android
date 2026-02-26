package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.manager.util.startAppSettingsActivity
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.components.toggle.UnlockWithPinState
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricSupportStatus
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher

@Suppress("LargeClass")
class AccountSecurityScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToDeleteAccountCalled = false
    private var onNavigateToPendingRequestsCalled = false
    private var onNavigateToUnlockSetupScreenCalled = false

    private val intentManager = mockk<IntentManager> {
        every { launchUri(any()) } just runs
        every { startActivity(any()) } returns true
    }
    private val captureBiometricsSuccess = slot<(cipher: Cipher?) -> Unit>()
    private val captureBiometricsCancel = slot<() -> Unit>()
    private val captureBiometricsLockOut = slot<() -> Unit>()
    private val captureBiometricsError = slot<() -> Unit>()
    private val biometricsManager: BiometricsManager = mockk {
        every { biometricSupportStatus } returns BiometricSupportStatus.CLASS_3_SUPPORTED
        every {
            promptBiometrics(
                onSuccess = capture(captureBiometricsSuccess),
                onCancel = capture(captureBiometricsCancel),
                onLockOut = capture(captureBiometricsLockOut),
                onError = capture(captureBiometricsError),
                cipher = CIPHER,
            )
        } just runs
    }
    private val mutableEventFlow = bufferedMutableSharedFlow<AccountSecurityEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AccountSecurityViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            biometricsManager = biometricsManager,
            intentManager = intentManager,
        ) {
            AccountSecurityScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToDeleteAccount = { onNavigateToDeleteAccountCalled = true },
                onNavigateToPendingRequests = { onNavigateToPendingRequestsCalled = true },
                onNavigateToSetupUnlockScreen = { onNavigateToUnlockSetupScreenCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on Log out click should send LogoutClick`() {
        composeTestRule.onNodeWithText("Log out").performScrollTo().performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.LogoutClick) }
    }

    @Test
    fun `on NavigateToApplicationDataSettings should launch the correct intent`() {
        mockkStatic(IntentManager::startAppSettingsActivity) {
            every { intentManager.startAppSettingsActivity() } returns true
            mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToApplicationDataSettings)
            verify(exactly = 1) { intentManager.startAppSettingsActivity() }
        }
    }

    @Test
    fun `on pending login requests click should send PendingLoginRequestsClick`() {
        composeTestRule
            .onNodeWithText("Pending login requests")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.PendingLoginRequestsClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on unlock with biometrics toggle should send EnableBiometricsClick when isUnlockWithBiometricsEnabled is false`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AccountSecurityAction.EnableBiometricsClick)
        }
    }

    @Test
    fun `on unlock with biometrics toggle should send UnlockWithBiometricToggleDisabled`() {
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = true) }
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggleDisabled)
        }
    }

    @Test
    fun `on unlock with biometrics toggle should un-toggle on cancel`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        mutableEventFlow.tryEmit(AccountSecurityEvent.ShowBiometricsPrompt(CIPHER))
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
        captureBiometricsCancel.captured()
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Test
    fun `on unlock with biometrics toggle should un-toggle on error`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        mutableEventFlow.tryEmit(AccountSecurityEvent.ShowBiometricsPrompt(CIPHER))
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
        captureBiometricsError.captured()
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Test
    fun `on unlock with biometrics toggle should un-toggle on lock out`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        mutableEventFlow.tryEmit(AccountSecurityEvent.ShowBiometricsPrompt(CIPHER))
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
        captureBiometricsLockOut.captured()
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        verify(exactly = 0) {
            viewModel.trySendAction(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on unlock with biometrics toggle should send UnlockWithBiometricToggleEnabled on success`() {
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        mutableEventFlow.tryEmit(AccountSecurityEvent.ShowBiometricsPrompt(CIPHER))
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOn()
        captureBiometricsSuccess.captured(CIPHER)
        composeTestRule
            .onNodeWithText("Unlock with Biometrics")
            .performScrollTo()
            .assertIsOff()
        verify(exactly = 1) {
            viewModel.trySendAction(AccountSecurityAction.UnlockWithBiometricToggleEnabled(CIPHER))
        }
    }

    @Test
    fun `on unlock with biometrics should be toggled on or off according to state`() {
        composeTestRule.onNodeWithText("Unlock with Biometrics").assertIsOff()
        mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = true) }
        composeTestRule.onNodeWithText("Unlock with Biometrics").assertIsOn()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlock option section should be displayed according to state if biometrics is available`() {
        val section = "UNLOCK OPTIONS"
        composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = true,
            )
        }
        composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = false,
            )
        }
        composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlock option section should be displayed according to state if biometrics is not available`() {
        coEvery {
            biometricsManager.biometricSupportStatus
        } returns BiometricSupportStatus.NOT_SUPPORTED
        val section = "UNLOCK OPTIONS"

        composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = true,
            )
        }
        composeTestRule.onNodeWithText(section).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = false,
            )
        }
        composeTestRule.onNodeWithText(section).assertDoesNotExist()
    }

    @Test
    fun `unlock with pin toggle should be displayed according to state`() {
        val toggleText = "Unlock with PIN code"
        composeTestRule.onNodeWithText(toggleText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = true,
            )
        }
        composeTestRule.onNodeWithText(toggleText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                removeUnlockWithPinPolicyEnabled = true,
                isUnlockWithPinEnabled = false,
            )
        }
        composeTestRule.onNodeWithText(toggleText).assertDoesNotExist()
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

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
            )
        }
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
            .onAllNodesWithText("Enter your PIN code")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "Your PIN must be at least 4 characters. Your PIN settings will be reset " +
                    "if you manually log out of the Bitwarden app.",
            )
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("PIN")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .assertIsFocused()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
            )
        }
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

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.Disabled),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `PIN input dialog with empty pin should disable the Submit button`() {
        mutableStateFlow.update {
            it.copy(isUnlockWithPinEnabled = false)
        }
        composeTestRule
            .onNodeWithText("Unlock with PIN code")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Submit")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotEnabled()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with non-empty pin and isUnlockWithPasswordEnabled true should show a confirmation dialog and send UnlockWithPinToggle PendingEnabled`() {
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
            .onAllNodesWithText("Require master password on app restart?")
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

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle(UnlockWithPinState.PendingEnabled),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PIN input dialog Submit click with non-empty pin and isUnlockWithPasswordEnabled false should show a confirmation dialog and send UnlockWithPinToggle Enabled`() {
        mutableStateFlow.update {
            it.copy(
                isUnlockWithPinEnabled = false,
                isUnlockWithPasswordEnabled = false,
            )
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

        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = false,
                    ),
                ),
            )
        }
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
                AccountSecurityAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = false,
                    ),
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
                AccountSecurityAction.UnlockWithPinToggle(
                    UnlockWithPinState.Enabled(
                        pin = "1234",
                        shouldRequireMasterPasswordOnRestart = true,
                    ),
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
    fun `session timeout support text should update according to state`() {
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.CUSTOM,
                ),
            )
        }
        val timeOnlyText = "Your organization has set the maximum session timeout " +
            "to 1 hour and 40 minutes."
        composeTestRule
            .onNodeWithText(timeOnlyText)
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.IMMEDIATELY,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "This setting is managed by your organization.")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.ON_APP_RESTART,
                ),
            )
        }
        val appRestartText = "Your organization has set the default session timeout " +
            "to on app restart."
        composeTestRule
            .onNodeWithText(text = appRestartText)
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.NEVER,
                ),
            )
        }
        val neverText = "Your organization has set the default session timeout to never."
        composeTestRule
            .onNodeWithText(text = neverText)
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.ON_SYSTEM_LOCK,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = appRestartText)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `session timeout action support text should update according to state`() {
        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = false,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = false,
                vaultTimeoutPolicy = null,
            )
        }
        composeTestRule
            .onNodeWithText(text = "Set up an unlock option to change your vault timeout action.")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = false,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = false,
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = PolicyInformation.VaultTimeout.Action.LOCK,
                    type = null,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "This setting is managed by your organization.")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = true,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = false,
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = PolicyInformation.VaultTimeout.Action.LOCK,
                    type = null,
                ),
            )
        }
        composeTestRule
            .onNodeWithText(text = "This setting is managed by your organization.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `session timeout should be enabled on or off according to state`() {
        composeTestRule
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = null,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.IMMEDIATELY,
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `session timeout action should be enabled on or off according to state`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = null,
                    action = PolicyInformation.VaultTimeout.Action.LOCK,
                    type = null,
                ),
            )
        }
        composeTestRule
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `session timeout should be updated on or off according to state`() {
        composeTestRule
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
            .performScrollTo()
            .assertIsDisplayed()
        mutableStateFlow.update { it.copy(vaultTimeout = VaultTimeout.FourHours) }
        composeTestRule
            .onNodeWithContentDescription(label = "4 hours. Session timeout")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `on session timeout click should show a selection dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
    fun `on session timeout click should update according to state`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.CUSTOM,
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onNodeWithText("4 hours")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("On app restart")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Never")
            .assertDoesNotExist()
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
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onAllNodesWithText(text = "Okay")
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
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onNodeWithContentDescription(label = "30 minutes. Session timeout")
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
            .onAllNodesWithText(text = "Okay")
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
            .onNode(hasTextExactly("Custom timeout", "0 minutes"))
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }

        composeTestRule
            .onNode(hasTextExactly("Custom timeout", "2 hours, 3 minutes"))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 1234))
        }

        composeTestRule
            .onNode(hasTextExactly("Custom timeout", "20 hours, 34 minutes"))
            .assertIsDisplayed()
    }

    @Test
    fun `custom session timeout click should show a time-picker dialog`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123))
        }
        composeTestRule
            .onNode(hasTextExactly("Custom timeout", "2 hours, 3 minutes"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Select time")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text = "Okay")
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
            .onNode(hasTextExactly("Custom timeout", "2 hours, 3 minutes"))
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
            .onNode(hasTextExactly("Custom timeout", "2 hours, 3 minutes"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Okay")
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

    @Suppress("MaxLineLength")
    @Test
    fun `custom session timeout dialog Ok click should dismiss the dialog and show an error if value exceeds policy limit`() {
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                vaultTimeout = VaultTimeout.Custom(vaultTimeoutInMinutes = 123),
                vaultTimeoutPolicy = VaultTimeoutPolicy(
                    minutes = 100,
                    action = null,
                    type = PolicyInformation.VaultTimeout.Type.CUSTOM,
                ),
            )
        }
        composeTestRule
            .onNode(hasTextExactly("Custom timeout", "2 hours, 3 minutes"))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule
            .onNodeWithText("Your vault timeout exceeds the restrictions set by your organization.")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                AccountSecurityAction.CustomVaultTimeoutSelect(
                    VaultTimeout.Custom(vaultTimeoutInMinutes = 100),
                ),
            )
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on session timeout action click should show a selection dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Session timeout action")
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
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Session timeout action")
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

    @Test
    fun `on session timeout action dialog Logout click should open a confirmation dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Set session timeout to “Log out”?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(
                text = "After the timeout period, you will be logged out. You will need to be " +
                    "connected to the internet to log in and access your vault again. Your " +
                    "settings and PIN saved on this device won’t change.",
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
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Set session timeout to “Log out”?")
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
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Set session timeout to “Log out”?")
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

    @Test
    fun `on session timeout action dialog cancel click should close the dialog`() {
        composeTestRule.assertNoDialogExists()

        composeTestRule
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .performClick()

        composeTestRule
            .onAllNodesWithText("Session timeout action")
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
            .onNodeWithContentDescription(label = "Lock. Session timeout action")
            .performScrollTo()
            .assertIsDisplayed()
        mutableStateFlow.update { it.copy(vaultTimeoutAction = VaultTimeoutAction.LOGOUT) }
        composeTestRule
            .onNodeWithContentDescription(label = "Log out. Session timeout action")
            .performScrollTo()
            .assertIsDisplayed()
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
    fun `Error dialog should be shown or hidden according to state`() {
        val title = "title"
        val message = "message"
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialog = AccountSecurityDialog.Error(
                    title = title.asText(),
                    message = message.asText(),
                ),
            )
        }
        composeTestRule
            .onNodeWithText(title)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(message)
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(dialog = null) }

        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `Error dialog dismiss should send DismissDialog`() {
        val title = "title"
        val message = "message"
        mutableStateFlow.update {
            it.copy(
                dialog = AccountSecurityDialog.Error(
                    title = title.asText(),
                    message = message.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
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

    @Test
    fun `loading dialog should be displayed according to state`() {
        val loadingMessage = "Loading"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(loadingMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(dialog = AccountSecurityDialog.Loading(loadingMessage.asText()))
        }

        composeTestRule
            .onNodeWithText("Loading")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
    }

    @Test
    fun `change master password row should be displayed according to state`() {
        val rowText = "Change master password"
        composeTestRule.onNodeWithText(rowText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update { it.copy(isUnlockWithPasswordEnabled = false) }

        composeTestRule.onNodeWithText(rowText).assertDoesNotExist()
    }

    @Test
    fun `lock now row should be displayed according to state`() {
        val rowText = "Lock now"
        composeTestRule.onNodeWithText(rowText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = true,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(rowText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = false,
                isUnlockWithPasswordEnabled = true,
                isUnlockWithPinEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(rowText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = false,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = true,
            )
        }

        composeTestRule.onNodeWithText(rowText).performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                isUnlockWithBiometricsEnabled = false,
                isUnlockWithPasswordEnabled = false,
                isUnlockWithPinEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(rowText).assertDoesNotExist()
    }

    @Test
    fun `sync with Bitwarden authenticator UI should be displayed according to state`() {
        val toggleText = "Allow authenticator syncing"
        composeTestRule.onNodeWithText(toggleText).assertDoesNotExist()

        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowEnableAuthenticatorSync = true) }
        composeTestRule.onNodeWithText(toggleText).performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText(toggleText).filterToOne(isToggleable()).assertIsOff()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                shouldShowEnableAuthenticatorSync = true,
                isAuthenticatorSyncChecked = true,
            )
        }
        composeTestRule.onNodeWithText(toggleText).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(toggleText).filterToOne(isToggleable()).assertIsOn()
    }

    @Test
    fun `sync with Bitwarden authenticator click should send AuthenticatorSyncToggle action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowEnableAuthenticatorSync = true) }
        composeTestRule
            .onNodeWithText("Allow authenticator syncing")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.AuthenticatorSyncToggle(true)) }
    }

    @Test
    fun `unlock action card should show when state is true and hide when false`() {
        composeTestRule
            .onNodeWithText("Get started")
            .assertDoesNotExist()
        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowUnlockActionCard = true) }
        composeTestRule
            .onNodeWithText("Get started")
            .assertIsDisplayed()
        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowUnlockActionCard = false) }
        composeTestRule
            .onNodeWithText("Get started")
            .assertDoesNotExist()
    }

    @Test
    fun `when unlock action card is visible clicking the cta button should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowUnlockActionCard = true) }
        composeTestRule
            .onNodeWithText("Get started")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(AccountSecurityAction.UnlockActionCardCtaClick) }
    }

    @Test
    fun `when unlock action card is visible clicking dismissing should send correct action`() {
        mutableStateFlow.update { DEFAULT_STATE.copy(shouldShowUnlockActionCard = true) }
        composeTestRule
            .onNodeWithContentDescription("Close")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AccountSecurityAction.UnlockActionCardDismiss) }
    }

    @Test
    fun `on NavigateToSetupUnlockScreen event invokes the correct lambda`() {
        mutableEventFlow.tryEmit(AccountSecurityEvent.NavigateToSetupUnlockScreen)
        assertTrue(onNavigateToUnlockSetupScreenCalled)
    }
}

private val CIPHER = mockk<Cipher>()
private const val USER_ID: String = "activeUserId"
private val DEFAULT_STATE = AccountSecurityState(
    dialog = null,
    fingerprintPhrase = "fingerprint-placeholder".asText(),
    isAuthenticatorSyncChecked = false,
    isUnlockWithBiometricsEnabled = false,
    isUnlockWithPasswordEnabled = true,
    isUnlockWithPinEnabled = false,
    userId = USER_ID,
    shouldShowEnableAuthenticatorSync = false,
    vaultTimeout = VaultTimeout.ThirtyMinutes,
    vaultTimeoutAction = VaultTimeoutAction.LOCK,
    vaultTimeoutPolicy = null,
    shouldShowUnlockActionCard = false,
    removeUnlockWithPinPolicyEnabled = false,
)
