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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class LandingScreenTest : BaseComposeTest() {
    private val resources
        get() = ApplicationProvider.getApplicationContext<Application>().resources

    @Test
    fun `continue button should be enabled or disabled according to the state`() {
        val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Continue").assertIsEnabled()

        mutableStateFlow.update { it.copy(isContinueButtonEnabled = false) }

        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun `continue button click should send ContinueButtonClick action`() {
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Continue").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LandingAction.ContinueButtonClick)
        }
    }

    @Test
    fun `remember me should be toggled on or off according to the state`() {
        val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Remember me").assertIsOff()

        mutableStateFlow.update { it.copy(isRememberMeEnabled = true) }

        composeTestRule.onNodeWithText("Remember me").assertIsOn()
    }

    @Test
    fun `remember me click should send RememberMeToggle action`() {
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule
            .onNodeWithText("Remember me")
            .performClick()
        verify {
            viewModel.trySendAction(LandingAction.RememberMeToggle(true))
        }
    }

    @Test
    fun `create account click should send CreateAccountClick action`() {
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Create account").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LandingAction.CreateAccountClick)
        }
    }

    @Test
    fun `email address should change according to state`() {
        val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }

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
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule.onNodeWithText("Email address").performTextInput(input)
        verify {
            viewModel.trySendAction(LandingAction.EmailInputChanged(input))
        }
    }

    @Test
    fun `NavigateToCreateAccount event should call onNavigateToCreateAccount`() {
        var onNavigateToCreateAccountCalled = false
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LandingEvent.NavigateToCreateAccount)
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = { onNavigateToCreateAccountCalled = true },
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        assertTrue(onNavigateToCreateAccountCalled)
    }

    @Test
    fun `NavigateToLogin event should call onNavigateToLogin`() {
        val testEmail = "test@test.com"

        var capturedEmail: String? = null

        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LandingEvent.NavigateToLogin(testEmail))
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }

        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = { },
                onNavigateToLogin = { email ->
                    capturedEmail = email
                },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }

        assertEquals(testEmail, capturedEmail)
    }

    @Test
    fun `NavigateToEnvironment event should call onNavigateToEvent`() {
        var onNavigateToEnvironmentCalled = false
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns flowOf(LandingEvent.NavigateToEnvironment)
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = { },
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = { onNavigateToEnvironmentCalled = true },
                viewModel = viewModel,
            )
        }
        assertTrue(onNavigateToEnvironmentCalled)
    }

    @Test
    fun `selecting environment should send EnvironmentOptionSelect action`() {
        val selectedEnvironment = Environment.Eu
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(DEFAULT_STATE)
        }

        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }

        // Clicking to open dialog
        composeTestRule
            .onNodeWithText(Environment.Us.label.toString(resources))
            .performClick()

        // Clicking item on dialog
        composeTestRule
            .onNodeWithText(selectedEnvironment.label.toString(resources))
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
        val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }

        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                errorDialogState = BasicDialogState.Shown(
                    title = "Error dialog title".asText(),
                    message = "Error dialog message".asText(),
                ),
            )
        }

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Error dialog title")
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
    fun `error dialog OK click should send ErrorDialogDismiss action`() {
        val viewModel = mockk<LandingViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns MutableStateFlow(
                DEFAULT_STATE.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = "title".asText(),
                        message = "message".asText(),
                    ),
                ),
            )
            every { trySendAction(LandingAction.ErrorDialogDismiss) } returns Unit
        }
        composeTestRule.setContent {
            LandingScreen(
                onNavigateToCreateAccount = {},
                onNavigateToLogin = { _ -> },
                onNavigateToEnvironment = {},
                viewModel = viewModel,
            )
        }
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(LandingAction.ErrorDialogDismiss) }
    }

    companion object {
        val DEFAULT_STATE = LandingState(
            emailInput = "",
            isContinueButtonEnabled = true,
            isRememberMeEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            errorDialogState = BasicDialogState.Hidden,
        )
    }
}
