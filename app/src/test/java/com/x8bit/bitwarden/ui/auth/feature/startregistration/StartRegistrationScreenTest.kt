package com.x8bit.bitwarden.ui.auth.feature.startregistration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
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

class StartRegistrationScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToCompleteRegistrationCalled = false
    private var onNavigateToCheckEmailCalled = false
    private var onNavigateToEnvironmentCalled = false

    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startCustomTabsActivity(any()) } just runs
        every { startActivity(any()) } just runs
    }

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<StartRegistrationEvent>()
    private val viewModel = mockk<StartRegistrationViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            StartRegistrationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToCompleteRegistration = { _, _ ->
                    onNavigateToCompleteRegistrationCalled = true
                },
                onNavigateToCheckEmail = { _ -> onNavigateToCheckEmailCalled = true },
                onNavigateToEnvironment = { onNavigateToEnvironmentCalled = true },
                intentManager = intentManager,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `close click should send CloseClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(CloseClick) }
    }

    @Test
    fun `NavigateBack event should invoke navigate back lambda`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `onNavigateToCompleteRegistration event should invoke navigate to complete registration`() {
        mutableEventFlow.tryEmit(
            StartRegistrationEvent.NavigateToCompleteRegistration(
                email = "email",
                verificationToken = "verificationToken",
            ),
        )
        assertTrue(onNavigateToCompleteRegistrationCalled)
    }

    @Test
    fun `NavigateToCheckEmail event should invoke navigate to check email`() {
        mutableEventFlow.tryEmit(
            StartRegistrationEvent.NavigateToCheckEmail(
                email = "email",
            ),
        )
        assertTrue(onNavigateToCheckEmailCalled)
    }

    @Test
    fun `NavigateToEnvironment event should invoke navigate to environment`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateToEnvironment)
        assertTrue(onNavigateToEnvironmentCalled)
    }

    @Test
    fun `NavigateToPrivacyPolicy event should invoke intent manager`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateToPrivacyPolicy)
        verify {
            intentManager.launchUri("https://bitwarden.com/privacy/".toUri())
        }
    }

    @Test
    fun `NavigateToTerms event should invoke intent manager`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateToTerms)
        verify {
            intentManager.launchUri("https://bitwarden.com/terms/".toUri())
        }
    }

    @Test
    fun `NavigateToUnsubscribe event should invoke intent manager`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateToUnsubscribe)
        verify {
            intentManager.launchUri("https://bitwarden.com/email-preferences/".toUri())
        }
    }

    @Test
    fun `email input change should send EmailInputChange action`() {
        composeTestRule.onNodeWithText("Email address").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(EmailInputChange(TEST_INPUT)) }
    }

    @Test
    fun `name input change should send NameInputChange action`() {
        composeTestRule.onNodeWithText("Name").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(StartRegistrationAction.NameInputChange(TEST_INPUT)) }
    }

    @Test
    fun `clicking OK on the error dialog should send ErrorDialogDismiss action`() {
        mutableStateFlow.update {
            it.copy(
                dialog = StartRegistrationDialog.Error(
                    BasicDialogState.Shown(
                        title = "title".asText(),
                        message = "message".asText(),
                    ),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText("Ok")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(StartRegistrationAction.ErrorDialogDismiss) }
    }

    @Test
    fun `when BasicDialogState is Shown should show dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialog = StartRegistrationDialog.Error(
                    BasicDialogState.Shown(
                        title = "title".asText(),
                        message = "message".asText(),
                    ),
                ),
            )
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    companion object {
        private const val TEST_INPUT = "input"
        private val DEFAULT_STATE = StartRegistrationState(
            emailInput = "",
            nameInput = "",
            isReceiveMarketingEmailsToggled = false,
            isContinueButtonEnabled = false,
            selectedEnvironmentType = Environment.Type.US,
            dialog = null,
        )
    }
}
