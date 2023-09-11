package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class LoginScreenTest : BaseComposeTest() {

    @Test
    fun `Not you text click should send NotYouButtonClick action`() {
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToLanding = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Not you?").performClick()
        verify {
            viewModel.trySendAction(LoginAction.NotYouButtonClick)
        }
    }

    @Test
    fun `password input change should send PasswordInputChanged action`() {
        val input = "input"
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToLanding = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Master password").performTextInput(input)
        verify {
            viewModel.trySendAction(LoginAction.PasswordInputChanged(input))
        }
    }

    @Test
    fun `NavigateToLanding should call onNavigateToLanding`() {
        var onNavigateToLandingCalled = false
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LoginEvent.NavigateToLanding)
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateToLanding = { onNavigateToLandingCalled = true },
                viewModel = viewModel,
            )
        }
        assertTrue(onNavigateToLandingCalled)
    }
}
