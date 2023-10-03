package com.x8bit.bitwarden.ui.auth.feature.login

import android.content.Intent
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
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
                onNavigateBack = {},
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
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Master password").performTextInput(input)
        verify {
            viewModel.trySendAction(LoginAction.PasswordInputChanged(input))
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        var onNavigateBackCalled = false
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LoginEvent.NavigateBack)
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
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToCaptcha should call intentHandler startActivity`() {
        val intentHandler = mockk<IntentHandler>(relaxed = true) {
            every { startActivity(any()) } returns Unit
        }
        val mockIntent = mockk<Intent>()
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LoginEvent.NavigateToCaptcha(mockIntent))
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
                onNavigateBack = {},
                intentHandler = intentHandler,
                viewModel = viewModel,
            )
        }
        verify { intentHandler.startActivity(mockIntent) }
    }
}
