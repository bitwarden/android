package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class TwoFactorLoginScreenTest : BaseComposeTest() {
    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { launchUri(any()) } just runs
    }
    private val nfcManager: NfcManager = mockk {
        every { start() } just runs
        every { stop() } just runs
    }
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<TwoFactorLoginEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<TwoFactorLoginViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            TwoFactorLoginScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
                intentManager = intentManager,
                nfcManager = nfcManager,
            )
        }
    }

    @Test
    fun `basicDialog should update according to state`() {
        composeTestRule.onNodeWithText("Error message").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Error(
                    title = null,
                    message = "Error message".asText(),
                ),
            )
        }

        composeTestRule.onNodeWithText("Error message").isDisplayed()
    }

    @Test
    fun `close button click should send CloseButtonClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.CloseButtonClick)
        }
    }

    @Test
    fun `code input change should send CodeInputChanged action`() {
        val input = "123456"
        composeTestRule.onNodeWithText("Verification code").performTextInput(input)
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.CodeInputChanged(input))
        }
    }

    @Test
    fun `continue button click should send ContinueButtonClick action`() {
        mutableStateFlow.update {
            it.copy(isContinueButtonEnabled = true)
        }
        composeTestRule.onNodeWithText("Continue").performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.ContinueButtonClick)
        }
    }

    @Test
    fun `continue button enabled state should update according to the state`() {
        composeTestRule.onNodeWithText("Continue").assertIsNotEnabled()

        mutableStateFlow.update {
            it.copy(isContinueButtonEnabled = true)
        }

        composeTestRule.onNodeWithText("Continue").assertIsEnabled()
    }

    @Test
    fun `continue button text should update according to the state`() {
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.DUO)
        }

        composeTestRule.onNodeWithText("Launch Duo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertDoesNotExist()
    }

    @Test
    fun `description text should update according to state`() {
        val emailDetails =
            "Enter the 6 digit verification code that was emailed to ex***@email.com."
        val authAppDetails = "Enter the 6 digit verification code from your authenticator app."
        composeTestRule.onNodeWithText(emailDetails).isDisplayed()
        composeTestRule.onNodeWithText(authAppDetails).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP)
        }

        composeTestRule.onNodeWithText(emailDetails).assertDoesNotExist()
        composeTestRule.onNodeWithText(authAppDetails).isDisplayed()
    }

    @Test
    fun `loadingOverlay should update according to state`() {
        composeTestRule.onNodeWithText("Loading...").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading("Loading...".asText()),
            )
        }

        composeTestRule.onNodeWithText("Loading...").isDisplayed()
    }

    @Test
    fun `remember me click should send RememberMeToggle action`() {
        composeTestRule.onNodeWithText("Remember me").performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.RememberMeToggle(true))
        }
    }

    @Test
    fun `remember me should be toggled on or off according to the state`() {
        composeTestRule.onNodeWithText("Remember me").assertIsOff()

        mutableStateFlow.update { it.copy(isRememberMeEnabled = true) }

        composeTestRule.onNodeWithText("Remember me").assertIsOn()
    }

    @Test
    fun `resend email button click should send ResendEmailClick action`() {
        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.EMAIL)
        }
        composeTestRule.onNodeWithText("Send verification code email again").performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick)
        }
    }

    @Test
    fun `resend email button visibility should update according to state`() {
        val buttonText = "Send verification code email again"
        composeTestRule.onNodeWithText(buttonText).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP)
        }
        composeTestRule.onNodeWithText(buttonText).assertIsNotDisplayed()
    }

    @Test
    fun `input field visibility should update according to state`() {
        composeTestRule.onNodeWithText("Verification code").assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.DUO)
        }
        composeTestRule.onNodeWithText("Verification code").assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.DUO_ORGANIZATION)
        }
        composeTestRule.onNodeWithText("Verification code").assertIsNotDisplayed()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.WEB_AUTH)
        }
        composeTestRule.onNodeWithText("Verification code").assertIsNotDisplayed()
    }

    @Test
    fun `options menu icon click should show the auth method options`() {
        composeTestRule.onNodeWithContentDescription("More").performClick()
        composeTestRule.onNodeWithText("Recovery code").assertIsDisplayed()
    }

    @Test
    fun `options menu option click should should send SelectAuthMethod and close the menu`() {
        composeTestRule.onNodeWithContentDescription("More").performClick()
        composeTestRule.onNodeWithText("Recovery code").performClick()
        verify {
            viewModel.trySendAction(
                TwoFactorLoginAction.SelectAuthMethod(TwoFactorAuthMethod.RECOVERY_CODE),
            )
        }
        composeTestRule.onNodeWithText("Recovery code").assertDoesNotExist()
    }

    @Test
    fun `title text should update according to state`() {
        composeTestRule.onNodeWithText("Email").isDisplayed()
        composeTestRule.onNodeWithText("Authenticator App").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP)
        }

        composeTestRule.onNodeWithText("Email").assertDoesNotExist()
        composeTestRule.onNodeWithText("Authenticator App").isDisplayed()
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateBack)
        TestCase.assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToCaptcha should call intentManager startCustomTabsActivity`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToCaptcha(mockUri))
        verify { intentManager.startCustomTabsActivity(mockUri) }
    }

    @Test
    fun `NavigateToDuo should call intentManager startCustomTabsActivity`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToDuo(mockUri))
        verify { intentManager.startCustomTabsActivity(mockUri) }
    }

    @Test
    fun `NavigateToDuoNavigateToWebAuth should call intentManager startCustomTabsActivity`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToWebAuth(mockUri))
        verify { intentManager.startCustomTabsActivity(mockUri) }
    }

    @Test
    fun `NavigateToRecoveryCode should launch the recovery code uri`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToRecoveryCode(mockUri))
        verify {
            intentManager.launchUri(mockUri)
        }
    }
}

private val DEFAULT_STATE = TwoFactorLoginState(
    authMethod = TwoFactorAuthMethod.EMAIL,
    availableAuthMethods = listOf(
        TwoFactorAuthMethod.EMAIL,
        TwoFactorAuthMethod.RECOVERY_CODE,
    ),
    codeInput = "",
    displayEmail = "ex***@email.com",
    dialogState = null,
    isContinueButtonEnabled = false,
    isRememberMeEnabled = false,
    captchaToken = null,
    email = "example@email.com",
    password = "password123",
    orgIdentifier = "orgIdentifier",
)
