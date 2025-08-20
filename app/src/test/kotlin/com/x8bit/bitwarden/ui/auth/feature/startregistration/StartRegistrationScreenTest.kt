package com.x8bit.bitwarden.ui.auth.feature.startregistration

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.performCustomAccessibilityAction
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

class StartRegistrationScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToCompleteRegistrationCalled = false
    private var onNavigateToCheckEmailCalled = false
    private var onNavigateToEnvironmentCalled = false

    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startCustomTabsActivity(any()) } just runs
        every { startActivity(any()) } returns true
        every { launchUri(any()) } just runs
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
        setContent(
            intentManager = intentManager,
        ) {
            StartRegistrationScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToCompleteRegistration = { _, _ ->
                    onNavigateToCompleteRegistrationCalled = true
                },
                onNavigateToCheckEmail = { _ -> onNavigateToCheckEmailCalled = true },
                onNavigateToEnvironment = { onNavigateToEnvironmentCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `close click should send CloseClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify { viewModel.trySendAction(StartRegistrationAction.CloseClick) }
    }

    @Test
    fun `on ShowSnackbar should display snackbar content`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        composeTestRule.onNodeWithText(text = message).assertDoesNotExist()
        mutableEventFlow.tryEmit(StartRegistrationEvent.ShowSnackbar(data = data))
        composeTestRule.onNodeWithText(text = message).assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Email address (required)").performTextInput(TEST_INPUT)
        verify { viewModel.trySendAction(StartRegistrationAction.EmailInputChange(TEST_INPUT)) }
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
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }
        composeTestRule
            .onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(StartRegistrationAction.ErrorDialogDismiss) }
    }

    @Test
    fun `when BasicDialogState is Shown should show dialog`() {
        mutableStateFlow.update {
            it.copy(
                dialog = StartRegistrationDialog.Error(
                    title = "title".asText(),
                    message = "message".asText(),
                ),
            )
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `clicking the server tool tip should send ServerGeologyHelpClickAction`() {
        composeTestRule
            .onNodeWithContentDescription("Help with server geolocations.")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(StartRegistrationAction.ServerGeologyHelpClick) }
    }

    @Test
    fun `when NavigateToServerSelectionInfo is observed event should invoke intent manager`() {
        mutableEventFlow.tryEmit(StartRegistrationEvent.NavigateToServerSelectionInfo)

        verify {
            intentManager.launchUri(
                uri = "https://bitwarden.com/help/server-geographies/".toUri(),
            )
        }
    }

    @Test
    fun `when environment selected in dialog should send EnvironmentTypeSelect action`() {
        val selectedEnvironment = Environment.Eu

        // Clicking to open dialog
        composeTestRule
            .onNodeWithText(Environment.Us.label)
            .performScrollTo()
            .performClick()

        // Clicking item on dialog
        composeTestRule
            .onNodeWithText(selectedEnvironment.label)
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify {
            viewModel.trySendAction(
                StartRegistrationAction.EnvironmentTypeSelect(selectedEnvironment.type),
            )
        }

        // Make sure dialog is hidden:
        composeTestRule
            .onNode(isDialog())
            .assertDoesNotExist()
    }

    @Test
    fun `when continue button clicked should send ContinueClick action`() {
        mutableStateFlow.update {
            it.copy(
                isContinueButtonEnabled = true,
            )
        }
        composeTestRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(StartRegistrationAction.ContinueClick) }
    }

    @Test
    fun `when unsubscribe custom action invoked should send UnsubscribeMarketingEmailsClick`() {
        composeTestRule
            .onNodeWithText(
                text = "Get emails from Bitwarden for announcements, advice, and " +
                    "research opportunities. Unsubscribe at any time.",
            )
            .performCustomAccessibilityAction("Unsubscribe")

        verify { viewModel.trySendAction(StartRegistrationAction.UnsubscribeMarketingEmailsClick) }
    }

    @Test
    fun `when terms and conditions custom action invoked should send TermsClick`() {
        composeTestRule
            .onNodeWithText("By continuing, you agree to the Terms of Service and Privacy Policy")
            .performCustomAccessibilityAction("Terms of Service")

        verify { viewModel.trySendAction(StartRegistrationAction.TermsClick) }
    }

    @Test
    fun `when privacy policy custom action invoked should send TermsClick`() {
        composeTestRule
            .onNodeWithText("By continuing, you agree to the Terms of Service and Privacy Policy")
            .performCustomAccessibilityAction("Privacy Policy")

        verify { viewModel.trySendAction(StartRegistrationAction.PrivacyPolicyClick) }
    }
}

private const val TEST_INPUT: String = "input"
private val DEFAULT_STATE: StartRegistrationState = StartRegistrationState(
    emailInput = "",
    nameInput = "",
    isReceiveMarketingEmailsToggled = false,
    isContinueButtonEnabled = false,
    selectedEnvironmentType = Environment.Type.US,
    dialog = null,
)
