package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.util.assertLockOrLogoutDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertLogoutConfirmationDialogIsDisplayed
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.assertSwitcherIsDisplayed
import com.x8bit.bitwarden.ui.util.assertSwitcherIsNotDisplayed
import com.x8bit.bitwarden.ui.util.performAccountClick
import com.x8bit.bitwarden.ui.util.performAccountIconClick
import com.x8bit.bitwarden.ui.util.performAccountLongClick
import com.x8bit.bitwarden.ui.util.performLockAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountClick
import com.x8bit.bitwarden.ui.util.performLogoutAccountConfirmationClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class LoginScreenTest : BaseComposeTest() {
    private val intentHandler = mockk<IntentHandler>(relaxed = true) {
        every { startCustomTabsActivity(any()) } returns Unit
    }
    private var onNavigateBackCalled = false
    private val mutableEventFlow = MutableSharedFlow<LoginEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LoginViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            LoginScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
                intentHandler = intentHandler,
            )
        }
    }

    @Test
    fun `account menu icon is present according to the state`() {
        composeTestRule.onNodeWithContentDescription("Account").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY))
        }

        composeTestRule.onNodeWithContentDescription("Account").assertIsDisplayed()
    }

    @Test
    fun `account menu icon click should show the account switcher`() {
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }

        composeTestRule.performAccountIconClick()

        composeTestRule.assertSwitcherIsDisplayed(
            accountSummaries = accountSummaries,
            isAddAccountButtonVisible = false,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send SwitchAccountClick and close switcher`() {
        // Show the account switcher
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }
        composeTestRule.performAccountIconClick()

        composeTestRule.performAccountClick(accountSummary = ACTIVE_ACCOUNT_SUMMARY)

        verify {
            viewModel.trySendAction(LoginAction.SwitchAccountClick(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.assertSwitcherIsNotDisplayed(
            accountSummaries = accountSummaries,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account long click in the account switcher should show the lock-or-logout dialog and close the switcher`() {
        // Show the account switcher
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.assertNoDialogExists()

        composeTestRule.performAccountLongClick(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )

        composeTestRule.assertLockOrLogoutDialogIsDisplayed(
            accountSummary = ACTIVE_ACCOUNT_SUMMARY,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `lock button click in the lock-or-logout dialog should send LockAccountClick action and close the dialog`() {
        // Show the lock-or-logout dialog
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)

        composeTestRule.performLockAccountClick()

        verify { viewModel.trySendAction(LoginAction.LockAccountClick(ACTIVE_ACCOUNT_SUMMARY)) }
        composeTestRule.assertNoDialogExists()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `logout button click in the lock-or-logout dialog should show the logout confirmation dialog and hide the lock-or-logout dialog`() {
        // Show the lock-or-logout dialog
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }
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
        val accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY)
        mutableStateFlow.update {
            it.copy(accountSummaries = accountSummaries)
        }
        composeTestRule.performAccountIconClick()
        composeTestRule.performAccountLongClick(ACTIVE_ACCOUNT_SUMMARY)
        composeTestRule.performLogoutAccountClick()

        composeTestRule.performLogoutAccountConfirmationClick()

        verify { viewModel.trySendAction(LoginAction.LogoutAccountClick(ACTIVE_ACCOUNT_SUMMARY)) }
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `close button click should send CloseButtonClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(LoginAction.CloseButtonClick)
        }
    }

    @Test
    fun `Not you text click should send NotYouButtonClick action`() {
        composeTestRule.onNodeWithText("Not you?").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LoginAction.NotYouButtonClick)
        }
    }

    @Test
    fun `master password hint text click should send MasterPasswordHintClick action`() {
        composeTestRule.onNodeWithText("Get your master password hint").performClick()
        verify {
            viewModel.trySendAction(LoginAction.MasterPasswordHintClick)
        }
    }

    @Test
    fun `master password hint option menu click should send MasterPasswordHintClick action`() {
        // Confirm dropdown version of item is absent
        composeTestRule
            .onAllNodesWithText("Get your master password hint")
            .filter(hasAnyAncestor(isPopup()))
            .assertCountEquals(0)
        // Open the overflow menu
        composeTestRule.onNodeWithContentDescription("More").performClick()
        // Click on the password hint item in the dropdown
        composeTestRule
            .onAllNodesWithText("Get your master password hint")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()
        verify {
            viewModel.trySendAction(LoginAction.MasterPasswordHintClick)
        }
    }

    @Test
    fun `password input change should send PasswordInputChanged action`() {
        val input = "input"
        composeTestRule.onNodeWithText("Master password").performTextInput(input)
        verify {
            viewModel.trySendAction(LoginAction.PasswordInputChanged(input))
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(LoginEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToCaptcha should call intentHandler startCustomTabsActivity`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(LoginEvent.NavigateToCaptcha(mockUri))
        verify { intentHandler.startCustomTabsActivity(mockUri) }
    }
}

private val ACTIVE_ACCOUNT_SUMMARY = AccountSummary(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environmentLabel = "bitwarden.com",
    isActive = true,
    isVaultUnlocked = true,
)

private val DEFAULT_STATE =
    LoginState(
        emailAddress = "",
        captchaToken = null,
        isLoginButtonEnabled = false,
        passwordInput = "",
        environmentLabel = "",
        loadingDialogState = LoadingDialogState.Hidden,
        errorDialogState = BasicDialogState.Hidden,
        accountSummaries = emptyList(),
    )
