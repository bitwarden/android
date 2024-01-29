package com.x8bit.bitwarden.ui.auth.feature.vaultunlock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.platform.manager.biometrics.BiometricsManager
import com.x8bit.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.x8bit.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.x8bit.bitwarden.ui.util.performAccountClick
import com.x8bit.bitwarden.ui.util.performAccountIconClick
import com.x8bit.bitwarden.ui.util.performAccountLongClick
import com.x8bit.bitwarden.ui.util.performAddAccountClick
import com.x8bit.bitwarden.ui.util.performLockAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountConfirmationClick
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

class VaultUnlockScreenTest : BaseComposeTest() {

    private val mutableEventFlow = bufferedMutableSharedFlow<VaultUnlockEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultUnlockViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val captureBiometricsSuccess = slot<() -> Unit>()
    private val captureBiometricsLockOut = slot<() -> Unit>()
    private val biometricsManager: BiometricsManager = mockk {
        every { isBiometricsSupported } returns true
        every {
            promptBiometrics(
                onSuccess = capture(captureBiometricsSuccess),
                onCancel = any(),
                onLockOut = capture(captureBiometricsLockOut),
                onError = any(),
            )
        } just runs
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultUnlockScreen(
                viewModel = viewModel,
                biometricsManager = biometricsManager,
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

        composeTestRule.performLogoutAccountConfirmationClick()

        verify {
            viewModel.trySendAction(VaultUnlockAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY))
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

    @Suppress("MaxLineLength")
    @Test
    fun `unlock with biometrics click should send BiometricsUnlockClick on biometrics authentication success`() {
        composeTestRule
            .onNodeWithText("Use biometrics to unlock")
            .performScrollTo()
            .performClick()
        captureBiometricsSuccess.captured()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsUnlockClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `unlock with biometrics click should send BiometricsLockOut on biometrics authentication lock out`() {
        composeTestRule
            .onNodeWithText("Use biometrics to unlock")
            .performScrollTo()
            .performClick()
        captureBiometricsLockOut.captured()
        verify(exactly = 1) {
            viewModel.trySendAction(VaultUnlockAction.BiometricsLockOut)
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

private val DEFAULT_STATE: VaultUnlockState = VaultUnlockState(
    accountSummaries = ACCOUNT_SUMMARIES,
    avatarColorString = "0000FF",
    dialog = null,
    email = "bit@bitwarden.com",
    environmentUrl = DEFAULT_ENVIRONMENT_URL,
    initials = "AU",
    input = "",
    isBiometricsValid = true,
    isBiometricEnabled = true,
    vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
)
