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
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class VaultUnlockScreenTest : BaseComposeTest() {

    private val mutableEventFlow = MutableSharedFlow<VaultUnlockEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<VaultUnlockViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            VaultUnlockScreen(
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `account icon click should show the account switcher`() {
        composeTestRule.onNodeWithText("active@bitwarden.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertDoesNotExist()
        composeTestRule.onNodeWithText("Add account").assertDoesNotExist()

        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("active@bitwarden.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add account").assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send AccountSwitchClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("locked@bitwarden.com").performClick()
        verify {
            viewModel.trySendAction(VaultUnlockAction.SwitchAccountClick(LOCKED_ACCOUNT_SUMMARY))
        }
        composeTestRule.onNodeWithText("locked@bitwarden.com").assertDoesNotExist()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `add account click in the account switcher should send AddAccountClick and close switcher`() {
        // Open the Account Switcher
        composeTestRule.onNodeWithText("AU").performClick()

        composeTestRule.onNodeWithText("Add account").performClick()
        verify { viewModel.trySendAction(VaultUnlockAction.AddAccountClick) }
        composeTestRule.onNodeWithText("Add account").assertDoesNotExist()
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
    fun `password input state change should update unlock button enabled`() {
        composeTestRule.onNodeWithText("Unlock").performScrollTo().assertIsNotEnabled()
        mutableStateFlow.update { it.copy(passwordInput = "a") }
        composeTestRule.onNodeWithText("Unlock").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `unlock click should send UnlockClick action`() {
        mutableStateFlow.update { it.copy(passwordInput = "abdc1234") }
        composeTestRule
            .onNodeWithText("Unlock")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(VaultUnlockAction.UnlockClick) }
    }

    @Test
    fun `master password change should send PasswordInputChanged action`() {
        val input = "abcd1234"
        composeTestRule
            .onNodeWithText("Master password")
            .performScrollTo()
            .performTextInput(input)
        verify {
            viewModel.trySendAction(VaultUnlockAction.PasswordInputChanged(input))
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
    status = AccountSummary.Status.ACTIVE,
)

private val LOCKED_ACCOUNT_SUMMARY = AccountSummary(
    userId = "lockedUserId",
    name = "Locked User",
    email = "locked@bitwarden.com",
    avatarColorHex = "#00aaaa",
    environmentLabel = "bitwarden.com",
    status = AccountSummary.Status.LOCKED,
)

private val DEFAULT_STATE: VaultUnlockState = VaultUnlockState(
    accountSummaries = persistentListOf(
        ACTIVE_ACCOUNT_SUMMARY,
        LOCKED_ACCOUNT_SUMMARY,
    ),
    avatarColorString = "0000FF",
    dialog = null,
    email = "bit@bitwarden.com",
    environmentUrl = DEFAULT_ENVIRONMENT_URL,
    initials = "AU",
    passwordInput = "",
)
