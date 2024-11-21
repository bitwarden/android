package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CredentialAssertionResult
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2GetCredentialsResult
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.autofill.fido2.manager.Fido2CompletionManager
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertRemovalConfirmationDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.x8bit.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.x8bit.bitwarden.ui.util.performAccountClick
import com.x8bit.bitwarden.ui.util.performAccountIconClick
import com.x8bit.bitwarden.ui.util.performAccountLongClick
import com.x8bit.bitwarden.ui.util.performAddAccountClick
import com.x8bit.bitwarden.ui.util.performLockAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountClick
import com.x8bit.bitwarden.ui.util.performRemoveAccountClick
import com.x8bit.bitwarden.ui.util.performYesDialogButtonClick
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import javax.crypto.Cipher

class VaultUnlockScreenTest : BaseComposeTest() {

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultUnlockEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultUnlockViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val captureBiometricsSuccess = slot<(cipher: Cipher?) -> Unit>()
    private val captureBiometricsLockOut = slot<() -> Unit>()
    private val biometricsManager: BiometricsManager = mockk {
        every { isBiometricsSupported } returns true
        every {
            promptBiometrics(
                onSuccess = capture(captureBiometricsSuccess),
                onCancel = any(),
                onLockOut = capture(captureBiometricsLockOut),
                onError = any(),
                cipher = CIPHER,
            )
        } just runs
    }
    private val fido2CompletionManager: Fido2CompletionManager = mockk {
        every { completeFido2Assertion(any()) } just runs
        every { completeFido2GetCredentialRequest(any()) } just runs
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultUnlockScreen(
                viewModel = viewModel,
                biometricsManager = biometricsManager,
                fido2CompletionManager = fido2CompletionManager,
            )
        }
    }

    @Test
    fun `on PromptForBiometrics should call promptBiometrics on biometricsManager`() {
        mutableEventFlow.tryEmit(VaultUnlockEvent.PromptForBiometrics(CIPHER))
        verify(exactly = 1) {
            biometricsManager.promptBiometrics(
                onSuccess = any(),
                onCancel = any(),
                onError = any(),
                onLockOut = any(),
                cipher = any(),
            )
        }
    }

    @Test
    fun `on biometrics authentication success should send BiometricsUnlockSuccess`() {
        mutableEventFlow.tryEmit(VaultUnlockEvent.PromptForBiometrics(CIPHER))
        captureBiometricsSuccess.captured(CIPHER)
        verify(exactly = 1) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockSuccess(CIPHER))
        }
    }

    @Test
    fun `on biometrics authentication lockout should send BiometricsLockOut`() {
        mutableEventFlow.tryEmit(VaultUnlockEvent.PromptForBiometrics(CIPHER))
        captureBiometricsLockOut.captured()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Fido2GetCredentialsError should call completeFido2GetCredentialRequest on fido2CompletionManager`() {
        mutableEventFlow.tryEmit(VaultUnlockEvent.Fido2GetCredentialsError)
        verify(exactly = 1) {
            fido2CompletionManager.completeFido2GetCredentialRequest(
                result = Fido2GetCredentialsResult.Error,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on Fido2AssertCredentialError should call completeFido2AssertCredential on fido2CompletionManager`() {
        mutableEventFlow.tryEmit(VaultUnlockEvent.Fido2CredentialAssertionError)
        verify(exactly = 1) {
            fido2CompletionManager.completeFido2Assertion(
                result = Fido2CredentialAssertionResult.Error,
            )
        }
    }

    @Test
    fun `account icon click should show the account switcher`() {
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )

        composeTestRule.performAccountIconClick()

        composeTestRule.assertSwitcherIsDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send AccountSwitchClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.performAccountIconClick()

        composeTestRule.performAccountClick(accountSummary = LOCKED_ACCOUNT_SUMMARY)

        verify {
            viewModel.trySendAction(VaultUnlockAction.SwitchAccountClick(LOCKED_ACCOUNT_SUMMARY))
        }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `add account click in the account switcher should send AddAccountClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.performAccountIconClick()

        composeTestRule.performAddAccountClick()

        verify { viewModel.trySendAction(VaultUnlockAction.AddAccountClick) }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = ACCOUNT_SUMMARIES,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account long click in the account switcher should show the lock-or-logout dialog and close the switcher`() {
        // Show the account switcher
        composeTestRule.performAccountIconClick()
        composeTestRule.assertNoDialogExists()

        composeTestRule.performAccountLongClick(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )

        composeTestRule.assertLockOrLogoutDialogIsDisplayed(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )
    }

    @Test
    fun `title should change according to state`() {
        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.MASTER_PASSWORD)
        }

        composeTestRule
            .onNodeWithText("Verify master password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Verify PIN")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.PIN)
        }

        composeTestRule
            .onNodeWithText("Verify master password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Verify PIN")
            .assertIsDisplayed()
    }

    @Test
    fun `message should change according to state`() {
        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.MASTER_PASSWORD)
        }

        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your master password to continue.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your PIN code to continue.")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.PIN)
        }

        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your master password to continue.")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your PIN code to continue.")
            .assertIsDisplayed()
    }

    @Test
    fun `input label should change according to state`() {
        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.MASTER_PASSWORD)
        }

        composeTestRule
            .onNodeWithText("Master password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("PIN")
            .assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(vaultUnlockType = VaultUnlockType.PIN)
        }

        composeTestRule
            .onNodeWithText("Master password")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("PIN")
            .assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `lock button click in the lock-or-logout dialog should send LockAccountClick action and close the dialog`() {
        // Show the lock-or-logout dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLockAccountClick()

        verify {
            viewModel.trySendAction(VaultUnlockAction.LockAccountClick(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the lock-or-logout dialog should show the logout confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLogoutAccountClick()

        composeTestRule.assertLogoutConfirmationDialogIsDisplayed(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the logout confirmation dialog should send LogoutAccountClick action and close the dialog`() {
        // Show the logout confirmation dialog
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)
        composeTestRule.performLogoutAccountClick()

        composeTestRule.performYesDialogButtonClick()

        verify {
            viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `remove account button click in the lock-or-logout dialog should show the remove account confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        val activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY.copy(isLoggedIn = false)
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(activeAccountSummary))
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(activeAccountSummary)

        composeTestRule.performRemoveAccountClick()

        composeTestRule.assertRemovalConfirmationDialogIsDisplayed(activeAccountSummary)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `remove account button click in the remove account confirmation dialog should send LogoutAccountClick action and close the dialog`() {
        // Show the remove account confirmation dialog
        val activeAccountSummary = ACTIVE_ACCOUNT_SUMMARY.copy(isLoggedIn = false)
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(activeAccountSummary))
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(activeAccountSummary)
        composeTestRule.performRemoveAccountClick()

        composeTestRule.performYesDialogButtonClick()

        verify {
            viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(activeAccountSummary))
        }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `logout click in the overflow menu should show the logout confirmation dialog`() {
        // Confirm neither the popup nor the dialog are showing
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        // Expand the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()
        composeTestRule.onNode(isPopup()).assertIsDisplayed()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        // Click on the logout item
        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        // Check for the dialog
        composeTestRule
            .onNode(isDialog())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Are you sure you want to log out?")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `Yes click in the logout confirmation dialog should send the ConfirmLogoutClick action`() {
        // Expand the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()

        // Click on the logout item to display the dialog
        composeTestRule
            .onAllNodesWithText("Log out")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        // Click on the Yes button in the dialog
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(VaultUnlockAction.ConfirmLogoutClick) }
    }

    @Test
    fun `email state change should update logged in as text`() {
        val newEmail = "david@bitwarden.com"
        val textBeforeUpdate = "Logged in as ${DEFAULT_STATE.email} on $DEFAULT_ENVIRONMENT_URL."
        val textAfterUpdate = "Logged in as $newEmail on $DEFAULT_ENVIRONMENT_URL."
        composeTestRule.onNodeWithText(textBeforeUpdate).assertExists()
        composeTestRule.onNodeWithText(textAfterUpdate).assertDoesNotExist()
        mutableStateFlow.update { it.copy(email = newEmail) }
        composeTestRule.onNodeWithText(textBeforeUpdate).assertDoesNotExist()
        composeTestRule.onNodeWithText(textAfterUpdate).assertExists()
    }

    @Test
    fun `environment url state change should update logged in as text`() {
        val newEnvironmentUrl = "eu.bitwarden.com"
        val textBeforeUpdate = "Logged in as ${DEFAULT_STATE.email} on $DEFAULT_ENVIRONMENT_URL."
        val textAfterUpdate = "Logged in as ${DEFAULT_STATE.email} on $newEnvironmentUrl."
        composeTestRule.onNodeWithText(textBeforeUpdate).assertExists()
        composeTestRule.onNodeWithText(textAfterUpdate).assertDoesNotExist()
        mutableStateFlow.update { it.copy(environmentUrl = newEnvironmentUrl) }
        composeTestRule.onNodeWithText(textBeforeUpdate).assertDoesNotExist()
        composeTestRule.onNodeWithText(textAfterUpdate).assertExists()
    }

    @Test
    fun `input state change should update unlock button enabled`() {
        composeTestRule.onNodeWithText("Unlock").performScrollTo().assertIsNotEnabled()
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule.onNodeWithText("Unlock").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `unlock click should send UnlockClick action`() {
        mutableStateFlow.update { it.copy(input = "abdc1234") }
        composeTestRule
            .onNodeWithText("Unlock")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
    }

    @Test
    fun `keyboard Done event should send UnlockClick action`() {
        val keyEvent = KeyEvent(
            NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, NativeKeyEvent.KEYCODE_ENTER),
        )
        composeTestRule
            .onNodeWithText("Master password")
            .performScrollTo()
            .requestFocus()
            .performKeyPress(keyEvent)
        verify { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
    }

    @Test
    fun `state with input and without biometrics should request focus on input field`() {
        mutableStateFlow.update { it.copy(hideInput = false, isBiometricEnabled = false) }
        composeTestRule
            .onNodeWithText("Master password")
            .performScrollTo()
            .assertIsFocused()
    }

    @Test
    fun `input change should send InputChanged action`() {
        val input = "abcd1234"
        composeTestRule
            .onNodeWithText("Master password")
            .performScrollTo()
            .performTextInput(input)
        verify {
            viewModel.trySendAction(VaultUnlockAction.InputChanged(input))
        }
    }

    @Test
    fun `unlock with biometrics click should send BiometricsUnlockClick`() {
        composeTestRule
            .onNodeWithText("Use biometrics to unlock")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `biometric invalidated message should display according to state`() {
        mutableStateFlow.update {
            it.copy(
                isBiometricsValid = false,
                showBiometricInvalidatedMessage = true,
            )
        }
        composeTestRule
            .onNodeWithText("Biometric unlock for this account is disabled pending verification of master password.")
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(showBiometricInvalidatedMessage = false) }
        composeTestRule
            .onNodeWithText("Biometric unlock for this account is disabled pending verification of master password.")
            .assertDoesNotExist()
    }

    @Test
    fun `account button should update according to state`() {
        mutableStateFlow.update { it.copy(showAccountMenu = true) }
        composeTestRule.onNodeWithText("AU").assertIsDisplayed()

        mutableStateFlow.update { it.copy(showAccountMenu = false) }
        composeTestRule.onNodeWithText("AU").assertDoesNotExist()
    }

    @Test
    fun `input field and unlock button should update according to state`() {
        mutableStateFlow.update { it.copy(hideInput = false) }
        composeTestRule.onNodeWithText("Master password").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your master password to continue.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Unlock").assertIsDisplayed()

        mutableStateFlow.update { it.copy(hideInput = true) }
        composeTestRule.onNodeWithText("Master password").assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Your vault is locked. Verify your master password to continue.")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Unlock").assertDoesNotExist()
    }

    @Test
    fun `biometrics not supported dialog shows correctly`() {
        mutableStateFlow.update {
            it.copy(dialog = VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported)
        }
        composeTestRule
            .onNodeWithText("Biometrics are no longer supported on this device")
            .assertIsDisplayed()
    }

    @Test
    fun `DismissBiometricsNoLongerSupportedDialog should be sent when dialog is dismissed`() {
        mutableStateFlow.update {
            it.copy(dialog = VaultUnlockState.VaultUnlockDialog.BiometricsNoLongerSupported)
        }
        composeTestRule
            .onNodeWithText("Biometrics are no longer supported on this device")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Ok")
            .performClick()

        verify {
            viewModel.trySendAction(VaultUnlockAction.DismissBiometricsNoLongerSupportedDialog)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when biometric is needed but no longer supported BiometricsNoLongerSupported action is sent`() {
        every { biometricsManager.isBiometricsSupported } returns false
        mutableStateFlow.update {
            it.copy(
                isBiometricEnabled = true,
                hasMasterPassword = false,
                vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
            )
        }
        composeTestRule.waitForIdle()
        verify {
            viewModel.trySendAction(VaultUnlockAction.BiometricsNoLongerSupported)
        }
    }
}

private const val DEFAULT_ENVIRONMENT_URL: String = "vault.bitwarden.com"

private val ACTIVE_ACCOUNT_SUMMARY = AccountSummary(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environmentLabel = "bitwarden.com",
    isActive = true,
    isLoggedIn = true,
    isVaultUnlocked = true,
)

private val LOCKED_ACCOUNT_SUMMARY = AccountSummary(
    userId = "lockedUserId",
    name = "Locked User",
    email = "locked@bitwarden.com",
    avatarColorHex = "#00aaaa",
    environmentLabel = "bitwarden.com",
    isActive = false,
    isLoggedIn = true,
    isVaultUnlocked = false,
)

private val ACCOUNT_SUMMARIES = listOf(
    ACTIVE_ACCOUNT_SUMMARY,
    LOCKED_ACCOUNT_SUMMARY,
)

private val CIPHER = mockk<Cipher>()
private val DEFAULT_STATE: VaultUnlockState = VaultUnlockState(
    accountSummaries = ACCOUNT_SUMMARIES,
    avatarColorString = "0000FF",
    dialog = null,
    email = "bit@bitwarden.com",
    environmentUrl = DEFAULT_ENVIRONMENT_URL,
    hideInput = false,
    initials = "AU",
    input = "",
    isBiometricsValid = true,
    isBiometricEnabled = true,
    showAccountMenu = true,
    showBiometricInvalidatedMessage = false,
    userId = ACTIVE_ACCOUNT_SUMMARY.userId,
    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
    hasMasterPassword = true,
)
