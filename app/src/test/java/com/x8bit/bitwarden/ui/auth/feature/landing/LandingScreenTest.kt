package com.x8bit.bitwarden.ui.auth.feature.landing

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class LandingScreenTest : BaseComposeTest() {
    private var capturedEmail: String? = null
    private var onNavigateToCreateAccountCalled = false
    private var onNavigateToLoginCalled = false
    private var onNavigateToEnvironmentCalled = false
    private val mutableEventFlow = MutableSharedFlow<LandingEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LandingViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val resources
        get() = ApplicationProvider.getApplicationContext<Application>().resources

    @Before
    fun setUp() {
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = { onNavigateToCreateAccountCalled = true },
                onNavigateToLogin = { capturedEmail ->
                    this.capturedEmail = capturedEmail
                    onNavigateToLoginCalled = true
                },
                onNavigateToEnvironment = { onNavigateToEnvironmentCalled = true },
                viewModel = viewModel,
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
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY))
        }

        composeTestRule.onNodeWithContentDescription("Account").performClick()

        composeTestRule.onNodeWithText("active@bitwarden.com").assertIsDisplayed()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account click in the account switcher should send SwitchAccountClick and close switcher`() {
        // Show the account switcher
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.onNodeWithContentDescription("Account").performClick()
        composeTestRule.onNodeWithText("active@bitwarden.com").assertIsDisplayed()

        composeTestRule.onNodeWithText("active@bitwarden.com").performClick()

        verify {
            viewModel.trySendAction(LandingAction.SwitchAccountClick(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.onNodeWithText("active@bitwarden.com").assertDoesNotExist()
    }

    @Test
    fun `add account button in the account switcher does not exist`() {
        // Show the account switcher
        mutableStateFlow.update {
            it.copy(accountSummaries = listOf(ACTIVE_ACCOUNT_SUMMARY))
        }
        composeTestRule.onNodeWithContentDescription("Account").performClick()
        composeTestRule.onNodeWithText("active@bitwarden.com").assertIsDisplayed()

        composeTestRule.onNodeWithText("Add account").assertDoesNotExist()
    }

    @Test
    fun `continue button should be enabled or disabled according to the state`() {
        composeTestRule.onNodeWithText("Continue").assertIsEnabled()

        mutableStateFlow.update { it.copy(isContinueButtonEnabled = false) }

        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun `continue button click should send ContinueButtonClick action`() {
        composeTestRule.onNodeWithText("Continue").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LandingAction.ContinueButtonClick)
        }
    }

    @Test
    fun `remember me should be toggled on or off according to the state`() {
        composeTestRule.onNodeWithText("Remember me").assertIsOff()

        mutableStateFlow.update { it.copy(isRememberMeEnabled = true) }

        composeTestRule.onNodeWithText("Remember me").assertIsOn()
    }

    @Test
    fun `remember me click should send RememberMeToggle action`() {
        composeTestRule
            .onNodeWithText("Remember me")
            .performClick()
        verify {
            viewModel.trySendAction(LandingAction.RememberMeToggle(true))
        }
    }

    @Test
    fun `create account click should send CreateAccountClick action`() {
        composeTestRule.onNodeWithText("Create account").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LandingAction.CreateAccountClick)
        }
    }

    @Test
    fun `email address should change according to state`() {
        composeTestRule
            .onNodeWithText("Email address")
            .assertTextEquals("Email address", "")

        mutableStateFlow.update { it.copy(emailInput = "test@bitwarden.com") }

        composeTestRule
            .onNodeWithText("Email address")
            .assertTextEquals("Email address", "test@bitwarden.com")
    }

    @Test
    fun `email address change should send EmailInputChanged action`() {
        val input = "email"
        composeTestRule.onNodeWithText("Email address").performTextInput(input)
        verify {
            viewModel.trySendAction(LandingAction.EmailInputChanged(input))
        }
    }

    @Test
    fun `NavigateToCreateAccount event should call onNavigateToCreateAccount`() {
        mutableEventFlow.tryEmit(LandingEvent.NavigateToCreateAccount)
        assertTrue(onNavigateToCreateAccountCalled)
    }

    @Test
    fun `NavigateToLogin event should call onNavigateToLogin`() {
        val testEmail = "test@test.com"

        mutableEventFlow.tryEmit(LandingEvent.NavigateToLogin(testEmail))

        assertEquals(testEmail, capturedEmail)
        assertTrue(onNavigateToLoginCalled)
    }

    @Test
    fun `NavigateToEnvironment event should call onNavigateToEvent`() {
        mutableEventFlow.tryEmit(LandingEvent.NavigateToEnvironment)
        assertTrue(onNavigateToEnvironmentCalled)
    }

    @Test
    fun `selecting environment should send EnvironmentOptionSelect action`() {
        val selectedEnvironment = Environment.Eu

        // Clicking to open dialog
        composeTestRule
            .onNodeWithText(Environment.Us.label)
            .performClick()

        // Clicking item on dialog
        composeTestRule
            .onNodeWithText(selectedEnvironment.label)
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(LandingAction.EnvironmentTypeSelect(selectedEnvironment.type))
        }

        // Make sure dialog is hidden:
        composeTestRule
            .onNode(isDialog())
            .assertDoesNotExist()
    }

    @Test
    fun `error dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = LandingState.DialogState.Error(
                    message = "Error dialog message".asText(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("An error has occurred.")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Error dialog message")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Ok")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `error dialog OK click should send DialogDismiss action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = LandingState.DialogState.Error(
                    message = "message".asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(LandingAction.DialogDismiss) }
    }

    @Test
    fun `account already added dialog should be shown or hidden according to the state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialog = LandingState.DialogState.AccountAlreadyAdded(
                    accountSummary = mockk(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Account already added")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Would you like to switch to it now?")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `account already added dialog Cancel click should send DialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = LandingState.DialogState.AccountAlreadyAdded(
                    accountSummary = mockk(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Cancel")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(LandingAction.DialogDismiss) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `account already added dialog Yes click should send ConfirmSwitchToMatchingAccountClick action`() {
        val accountSummary = mockk<AccountSummary>()
        mutableStateFlow.update {
            it.copy(
                dialog = LandingState.DialogState.AccountAlreadyAdded(
                    accountSummary = accountSummary,
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                LandingAction.ConfirmSwitchToMatchingAccountClick(account = accountSummary),
            )
        }
    }
}

private val ACTIVE_ACCOUNT_SUMMARY = AccountSummary(
    userId = "activeUserId",
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    status = AccountSummary.Status.ACTIVE,
)

private val DEFAULT_STATE = LandingState(
    emailInput = "",
    isContinueButtonEnabled = true,
    isRememberMeEnabled = false,
    selectedEnvironmentType = Environment.Type.US,
    dialog = null,
    accountSummaries = emptyList(),
)
