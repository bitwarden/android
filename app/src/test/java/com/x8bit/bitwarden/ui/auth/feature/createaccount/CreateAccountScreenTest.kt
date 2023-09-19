package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class CreateAccountScreenTest : BaseComposeTest() {

    @Test
    fun `submit click should send SubmitClick action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SubmitClick) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Submit").performClick()
        verify { viewModel.trySendAction(SubmitClick) }
    }

    @Test
    fun `email input change should send EmailInputChange action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(EmailInputChange("input")) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Email address").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(EmailInputChange(TEST_INPUT)) }
    }

    @Test
    fun `password input change should send PasswordInputChange action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(PasswordInputChange("input")) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Master password").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `confirm password input change should send ConfirmPasswordInputChange action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(ConfirmPasswordInputChange("input")) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Re-type master password").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(ConfirmPasswordInputChange(TEST_INPUT)) }
    }

    @Test
    fun `password hint input change should send PasswordHintChange action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(PasswordHintChange("input")) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(viewModel)
        }
        composeTestRule
            .onNodeWithText("Master password hint (optional)")
            .performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordHintChange(TEST_INPUT)) }
    }

    companion object {
        private const val TEST_INPUT = "input"
        private val DEFAULT_STATE = CreateAccountState(
            emailInput = "",
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
        )
    }
}
