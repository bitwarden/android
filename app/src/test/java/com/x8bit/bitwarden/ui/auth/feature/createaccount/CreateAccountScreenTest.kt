package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class CreateAccountScreenTest : BaseComposeTest() {

    @Test
    fun `app bar submit click should send SubmitClick action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SubmitClick) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule.onAllNodesWithText("Submit")[0].performClick()
        verify { viewModel.trySendAction(SubmitClick) }
    }

    @Test
    fun `bottom button submit click should send SubmitClick action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(SubmitClick) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule.onAllNodesWithText("Submit")[1].performClick()
        verify { viewModel.trySendAction(SubmitClick) }
    }

    @Test
    fun `close click should send CloseClick action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(CloseClick) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(CloseClick) }
    }

    @Test
    fun `NavigateBack event should invoke navigate back lambda`() {
        var onNavigateBackCalled = false
        val onNavigateBack = { onNavigateBackCalled = true }
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns flowOf(CreateAccountEvent.NavigateBack)
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = onNavigateBack, viewModel = viewModel)
        }
        assert(onNavigateBackCalled)
    }

    @Test
    fun `email input change should send EmailInputChange action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
            every { eventFlow } returns emptyFlow()
            every { trySendAction(EmailInputChange("input")) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
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
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
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
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
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
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule
            .onNodeWithText("Master password hint (optional)")
            .performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(PasswordHintChange(TEST_INPUT)) }
    }

    @Test
    fun `clicking OK on the error dialog should send ErrorDialogDismiss action`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(
                DEFAULT_STATE.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = "title".asText(),
                        message = "message".asText(),
                    ),
                ),
            )
            every { eventFlow } returns emptyFlow()
            every { trySendAction(CreateAccountAction.ErrorDialogDismiss) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(CreateAccountAction.ErrorDialogDismiss) }
    }

    @Test
    fun `when BasicDialogState is Shown should show dialog`() {
        val viewModel = mockk<CreateAccountViewModel>(relaxed = true) {
            every { stateFlow } returns MutableStateFlow(
                DEFAULT_STATE.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = "title".asText(),
                        message = "message".asText(),
                    ),
                ),
            )
            every { eventFlow } returns emptyFlow()
            every { trySendAction(CreateAccountAction.ErrorDialogDismiss) } returns Unit
        }
        composeTestRule.setContent {
            CreateAccountScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    companion object {
        private const val TEST_INPUT = "input"
        private val DEFAULT_STATE = CreateAccountState(
            emailInput = "",
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            errorDialogState = BasicDialogState.Hidden,
        )
    }
}
