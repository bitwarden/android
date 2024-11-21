package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CheckDataBreachesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
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

class CreateAccountScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToLoginCalled = false

    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startCustomTabsActivity(any()) } just runs
        every { startActivity(any()) } just runs
    }

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<CreateAccountEvent>()
    private val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            CreateAccountScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToLogin = { _, _ -> onNavigateToLoginCalled = true },
                intentManager = intentManager,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `app bar submit click should send SubmitClick action`() {
        composeTestRule.onNodeWithText("Submit").performClick()
        verify { viewModel.trySendAction(SubmitClick) }
    }

    @Test
    fun `close click should send CloseClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(CloseClick) }
    }

    @Test
    fun `check data breaches click should send CheckDataBreachesToggle action`() {
        composeTestRule
            .onNodeWithText("Check known data breaches for this password")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(CheckDataBreachesToggle(true)) }
    }

    @Test
    fun `accept policies should be toggled on or off according to the state`() {
        composeTestRule
            .onNodeWithText("By activating this switch you agree", substring = true)
            .assertIsOff()

        mutableStateFlow.update { it.copy(isAcceptPoliciesToggled = true) }

        composeTestRule
            .onNodeWithText("By activating this switch you agree", substring = true)
            .assertIsOn()
    }

    @Test
    fun `accept policies click should send AcceptPoliciesToggle action`() {
        composeTestRule
            .onNodeWithText("By activating this switch you agree", substring = true)
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AcceptPoliciesToggle(true)) }
    }

    @Test
    fun `NavigateBack event should invoke navigate back lambda`() {
        mutableEventFlow.tryEmit(CreateAccountEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToLogin event should invoke navigate login lambda`() {
        mutableEventFlow.tryEmit(CreateAccountEvent.NavigateToLogin(email = "", captchaToken = ""))
        assertTrue(onNavigateToLoginCalled)
    }

    @Test
    fun `NavigateToCaptcha event should invoke intent manager`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(CreateAccountEvent.NavigateToCaptcha(uri = mockUri))
        verify {
            intentManager.startCustomTabsActivity(mockUri)
        }
    }

    @Test
    fun `NavigateToPrivacyPolicy event should invoke intent manager`() {
        mutableEventFlow.tryEmit(CreateAccountEvent.NavigateToPrivacyPolicy)
        verify {
            intentManager.launchUri("https://bitwarden.com/privacy/".toUri())
        }
    }

    @Test
    fun `NavigateToTerms event should invoke intent manager`() {
        mutableEventFlow.tryEmit(CreateAccountEvent.NavigateToTerms)
        verify {
            intentManager.launchUri("https://bitwarden.com/terms/".toUri())
        }
    }

    @Test
    fun `email input change should send EmailInputChange action`() {
        composeTestRule.onNodeWithText("Email address").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(EmailInputChange(TEST_INPUT)) }
    }

    @Test
    fun `password input change should send PasswordInputChange action`() {
        composeTestRule.onNodeWithText("Master password").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `confirm password input change should send ConfirmPasswordInputChange action`() {
        composeTestRule.onNodeWithText("Re-type master password").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(ConfirmPasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `password hint input change should send PasswordHintChange action`() {
        composeTestRule
            .onNodeWithText("Master password hint (optional)")
            .performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordHintChange(TEST_INPUT)) }
    }

    @Test
    fun `clicking OK on the error dialog should send ErrorDialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = CreateAccountDialog.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.ErrorDialogDismiss) }
    }

    @Test
    fun `clicking No on the HIBP dialog should send ErrorDialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(dialog = createHaveIBeenPwned())
        }
        composeTestRule
            .onAllNodesWithText("No")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.ErrorDialogDismiss) }
    }

    @Test
    fun `clicking Yes on the HIBP dialog should send ContinueWithBreachedPasswordClick action`() {
        mutableStateFlow.update {
            it.copy(dialog = createHaveIBeenPwned())
        }
        composeTestRule
            .onAllNodesWithText("Yes")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.ContinueWithBreachedPasswordClick) }
    }

    @Test
    fun `when BasicDialogState is Shown should show dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialog = CreateAccountDialog.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `password strength should change as state changes`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(passwordStrengthState = PasswordStrengthState.WEAK_1)
        }
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(passwordStrengthState = PasswordStrengthState.WEAK_2)
        }
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(passwordStrengthState = PasswordStrengthState.WEAK_3)
        }
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(passwordStrengthState = PasswordStrengthState.GOOD)
        }
        composeTestRule.onNodeWithText("Good").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(passwordStrengthState = PasswordStrengthState.STRONG)
        }
        composeTestRule.onNodeWithText("Strong").assertIsDisplayed()
    }

    @Test
    fun `toggling one password field visibility should toggle the other`() {
        // should start with 2 Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)[0]
            .performClick()

        // after clicking there should be no Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(0)

        // and there should be 2 hide buttons now, and we'll click the second one:
        composeTestRule
            .onAllNodesWithContentDescription("Hide")
            .assertCountEquals(2)[1]
            .performClick()

        // then there should be two show buttons again
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)
    }

    @Test
    fun `terms of service click should send TermsClick action`() {
        composeTestRule
            .onNodeWithText("Terms of Service")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.TermsClick) }
    }

    @Test
    fun `privacy policy click should send PrivacyPolicyClick action`() {
        composeTestRule
            .onNodeWithText("Privacy Policy")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.PrivacyPolicyClick) }
    }

    companion object {
        private const val TEST_INPUT = "input"
        private val DEFAULT_STATE = CreateAccountState(
            emailInput = "",
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isCheckDataBreachesToggled = false,
            isAcceptPoliciesToggled = false,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.NONE,
        )
    }
}
