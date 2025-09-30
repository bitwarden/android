package com.x8bit.bitwarden.ui.auth.feature.completeregistration

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.BackClick
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.CheckDataBreachesToggle
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ContinueWithBreachedPasswordClick
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.CompleteRegistrationAction.PasswordInputChange
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

class CompleteRegistrationScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToPreventAccountLockoutCalled = false
    private var onNavigateToPasswordGuidanceCalled = false
    private var onNavigateToLoginCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<CompleteRegistrationEvent>()
    private val viewModel = mockk<CompleteRegistrationViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        setContent {
            CompleteRegistrationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToPasswordGuidance = { onNavigateToPasswordGuidanceCalled = true },
                onNavigateToPreventAccountLockout = {
                    onNavigateToPreventAccountLockoutCalled = true
                },
                onNavigateToLogin = { email ->
                    onNavigateToLoginCalled = true
                    assertTrue(email == EMAIL)
                },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on ShowSnackbar should display snackbar content`() {
        val message = "message"
        composeTestRule.onNodeWithText(text = message).assertDoesNotExist()
        mutableEventFlow.tryEmit(CompleteRegistrationEvent.ShowSnackbar(message = message.asText()))
        composeTestRule.onNodeWithText(text = message).assertIsDisplayed()
    }

    @Test
    fun `close click should send CloseClick action`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(BackClick) }
    }

    @Test
    fun `check data breaches click should send CheckDataBreachesToggle action`() {
        composeTestRule
            .onNodeWithText("Check known data breaches for this password")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(CheckDataBreachesToggle(false)) }
    }

    @Test
    fun `NavigateBack event should invoke navigate back lambda`() {
        mutableEventFlow.tryEmit(CompleteRegistrationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `system back event should send BackClick action`() {
        backDispatcher?.onBackPressed()
        verify { viewModel.trySendAction(BackClick) }
    }

    @Test
    fun `password input change should send PasswordInputChange action`() {
        composeTestRule.onNodeWithText("Master password (required)").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `confirm password input change should send ConfirmPasswordInputChange action`() {
        composeTestRule.onNodeWithText("Re-type master password (required)")
            .performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(ConfirmPasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `password hint input change should send PasswordHintChange action`() {
        composeTestRule
            .onNodeWithText("Master password hint")
            .performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordHintChange(TEST_INPUT)) }
    }

    @Test
    fun `clicking OK on the error dialog should send ErrorDialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = CompleteRegistrationDialog.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(ErrorDialogDismiss) }
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
        verify { viewModel.trySendAction(ErrorDialogDismiss) }
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
        verify { viewModel.trySendAction(ContinueWithBreachedPasswordClick) }
    }

    @Test
    fun `when BasicDialogState is Shown should show dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialog = CompleteRegistrationDialog.Error(
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
            DEFAULT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.WEAK_1,
                passwordInput = "123123123123",
            )
        }
        composeTestRule.onNodeWithText("Weak").performScrollTo().assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.WEAK_2,
                passwordInput = "123123123123",
            )
        }
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.WEAK_3,
                passwordInput = "123123123123",
            )
        }
        composeTestRule.onNodeWithText("Weak").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.GOOD,
                passwordInput = "123123123123",
            )
        }
        composeTestRule.onNodeWithText("Good").assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                passwordStrengthState = PasswordStrengthState.STRONG,
                passwordInput = "123123123123",
            )
        }
        composeTestRule.onNodeWithText("Strong").assertIsDisplayed()
    }

    @Test
    fun `toggling one password field visibility should toggle the other`() {
        // should start with 2 Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)[0]
            .performScrollTo()
            .performClick()

        // after clicking there should be no Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(0)

        // and there should be 2 hide buttons now, and we'll click the second one:
        composeTestRule
            .onAllNodesWithContentDescription("Hide")
            .assertCountEquals(2)[1]
            .performScrollTo()
            .performClick()

        // then there should be two show buttons again
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `NavigateToPreventAccountLockout event should invoke navigate to prevent account lockout lambda`() {
        mutableEventFlow.tryEmit(CompleteRegistrationEvent.NavigateToPreventAccountLockout)
        assertTrue(onNavigateToPreventAccountLockoutCalled)
    }

    @Test
    fun `NavigateToPasswordGuidance event should invoke navigate to password guidance lambda`() {
        mutableEventFlow.tryEmit(CompleteRegistrationEvent.NavigateToMakePasswordStrong)
        assertTrue(onNavigateToPasswordGuidanceCalled)
    }

    @Test
    fun `NavigateToLogin event should invoke navigate to login lambda`() {
        mutableEventFlow.tryEmit(
            CompleteRegistrationEvent.NavigateToLogin(
                email = EMAIL,
            ),
        )

        assertTrue(onNavigateToLoginCalled)
    }

    // New Onboarding UI tests
    @Test
    fun `determine if using the new ui by title text`() {
        composeTestRule
            .onNode(hasText("Create account") and !hasClickAction())
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Set password")
            .assertDoesNotExist()
    }

    @Test
    fun `call to action state should update with input based on if both fields are populated`() {
        mutableStateFlow.update {
            it.copy(
                passwordInput = "",
                confirmPasswordInput = "password1234",
            )
        }
        composeTestRule
            .onNodeWithText("Next")
            .assertIsNotEnabled()
            .performScrollTo()
            .performClick()
        verify(exactly = 0) {
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        }

        mutableStateFlow.update {
            it.copy(
                passwordInput = "password1234",
                confirmPasswordInput = "password1234",
            )
        }

        composeTestRule
            .onNodeWithText("Next")
            .assertIsEnabled()
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(CompleteRegistrationAction.CallToActionClick)
        }
    }

    @Test
    fun `Click on action card should send MakePasswordStrongClick action`() {
        composeTestRule
            .onNodeWithText("Learn more")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(CompleteRegistrationAction.MakePasswordStrongClick) }
    }

    @Test
    fun `Click on prevent account lockout should send LearnToPreventLockoutClick action`() {
        composeTestRule
            .onNodeWithText("Learn about other ways to prevent account lockout")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(CompleteRegistrationAction.LearnToPreventLockoutClick)
        }
    }

    companion object {
        private const val EMAIL = "test@test.com"
        private const val TOKEN = "token"
        private const val TEST_INPUT = "input"
        private val DEFAULT_STATE = CompleteRegistrationState(
            userEmail = EMAIL,
            emailVerificationToken = TOKEN,
            fromEmail = true,
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isCheckDataBreachesToggled = true,
            dialog = null,
            passwordStrengthState = PasswordStrengthState.NONE,
            minimumPasswordLength = 12,
        )
    }
}
