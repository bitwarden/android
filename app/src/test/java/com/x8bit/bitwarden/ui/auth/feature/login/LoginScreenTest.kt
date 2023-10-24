package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
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
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
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
    fun `close button click should send CloseButtonClick action`() {
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(LoginAction.CloseButtonClick)
        }
    }

    @Test
    fun `Not you text click should send NotYouButtonClick action`() {
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Not you?").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LoginAction.NotYouButtonClick)
        }
    }

    @Test
    fun `master password hint text click should send MasterPasswordHintClick action`() {
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Get your master password hint").performClick()
        verify {
            viewModel.trySendAction(LoginAction.MasterPasswordHintClick)
        }
    }

    @Test
    fun `master password hint option menu click should send MasterPasswordHintClick action`() {
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
                ),
            )
        }
        composeTestRule.setContent {
            LoginScreen(
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
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
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
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
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
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
    fun `NavigateToCaptcha should call intentHandler startCustomTabsActivity`() {
        val intentHandler = mockk<IntentHandler>(relaxed = true) {
            every { startCustomTabsActivity(any()) } returns Unit
        }
        val mockUri = mockk<Uri>()
        val viewModel = mockk<LoginViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LoginEvent.NavigateToCaptcha(mockUri))
            every { stateFlow } returns MutableStateFlow(
                LoginState(
                    emailAddress = "",
                    captchaToken = null,
                    isLoginButtonEnabled = false,
                    passwordInput = "",
                    environmentLabel = "".asText(),
                    loadingDialogState = LoadingDialogState.Hidden,
                    errorDialogState = BasicDialogState.Hidden,
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
        verify { intentHandler.startCustomTabsActivity(mockUri) }
    }
}
