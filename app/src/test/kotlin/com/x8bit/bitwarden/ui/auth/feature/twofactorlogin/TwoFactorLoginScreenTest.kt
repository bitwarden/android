package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.manager.nfc.NfcManager
import com.x8bit.bitwarden.ui.platform.model.AuthTabLaunchers
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

class TwoFactorLoginScreenTest : BitwardenComposeTest() {
    private val duoLauncher: ActivityResultLauncher<Intent> = mockk()
    private val webAuthnLauncher: ActivityResultLauncher<Intent> = mockk()
    private val intentManager = mockk<IntentManager> {
        every { launchUri(uri = any()) } just runs
        every { startAuthTab(uri = any(), redirectScheme = any(), launcher = any()) } just runs
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
        setContent(
            authTabLaunchers = AuthTabLaunchers(
                duo = duoLauncher,
                sso = mockk(),
                webAuthn = webAuthnLauncher,
            ),
            intentManager = intentManager,
            nfcManager = nfcManager,
        ) {
            TwoFactorLoginScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on ShowSnackbar should display snackbar content`() {
        val message = "message"
        val data = BitwardenSnackbarData(message = message.asText())
        composeTestRule.onNodeWithText(text = message).assertDoesNotExist()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.ShowSnackbar(data = data))
        composeTestRule.onNodeWithText(text = message).assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Error message").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Continue")
            .performScrollTo()
            .performClick()
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
        composeTestRule.onNodeWithText("Continue")
            .performScrollTo()
            .assertIsDisplayed()

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
        composeTestRule.onNodeWithText(emailDetails).assertIsDisplayed()
        composeTestRule.onNodeWithText(authAppDetails).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP)
        }

        composeTestRule.onNodeWithText(emailDetails).assertDoesNotExist()
        composeTestRule.onNodeWithText(authAppDetails).assertIsDisplayed()
    }

    @Test
    fun `loadingOverlay should update according to state`() {
        composeTestRule.onNodeWithText("Loading...").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading("Loading...".asText()),
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun `remember me click should send RememberMeToggle action`() {
        composeTestRule.onNodeWithText("Remember").performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.RememberMeToggle(true))
        }
    }

    @Test
    fun `remember me should be toggled on or off according to the state`() {
        composeTestRule.onNodeWithText("Remember").assertIsOff()

        mutableStateFlow.update { it.copy(isRememberEnabled = true) }

        composeTestRule.onNodeWithText("Remember").assertIsOn()
    }

    @Test
    fun `resend email button click should send ResendEmailClick action`() {
        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.EMAIL)
        }
        composeTestRule.onNodeWithText("Resend code")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(TwoFactorLoginAction.ResendEmailClick)
        }
    }

    @Test
    fun `resend email button visibility should update according to state`() {
        val buttonText = "Resend code"
        composeTestRule.onNodeWithText(buttonText)
            .performScrollTo()
            .assertIsDisplayed()

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
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Authenticator app").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.AUTHENTICATOR_APP)
        }

        composeTestRule.onNodeWithText("Email").assertDoesNotExist()
        composeTestRule.onNodeWithText("Authenticator app").assertIsDisplayed()
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateBack)
        TestCase.assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToDuo should call intentManager startAuthTab`() {
        val mockUri = mockk<Uri>()
        val scheme = "bitwarden"
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToDuo(mockUri, scheme))
        verify(exactly = 1) {
            intentManager.startAuthTab(
                uri = mockUri,
                redirectScheme = scheme,
                launcher = duoLauncher,
            )
        }
    }

    @Test
    fun `NavigateToWebAuth should call intentManager startCustomTabsActivity`() {
        val mockUri = mockk<Uri>()
        val scheme = "bitwarden"
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToWebAuth(mockUri, scheme))
        verify(exactly = 1) {
            intentManager.startAuthTab(
                uri = mockUri,
                redirectScheme = scheme,
                launcher = webAuthnLauncher,
            )
        }
    }

    @Test
    fun `NavigateToRecoveryCode should launch the recovery code uri`() {
        val mockUri = mockk<Uri>()
        mutableEventFlow.tryEmit(TwoFactorLoginEvent.NavigateToRecoveryCode(mockUri))
        verify {
            intentManager.launchUri(mockUri)
        }
    }

    @Test
    fun `remember me should not be visible if isNewDeviceVerification is true`() {
        mutableStateFlow.update {
            it.copy(isNewDeviceVerification = true)
        }
        composeTestRule.onNodeWithText("Remember").assertIsNotDisplayed()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `if isNewDeviceVerification is true description should contain We don't recognize this device string`() {
        mutableStateFlow.update {
            it.copy(authMethod = TwoFactorAuthMethod.EMAIL, isNewDeviceVerification = true)
        }
        composeTestRule.onNode(
            hasText(
                text = "We don’t recognize this device",
                substring = true,
                ignoreCase = true,
            ),
        ).assertIsDisplayed()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `if isNewDeviceVerification is false description should not contain We don't recognize this device string`() {
        mutableStateFlow.update {
            it.copy(isNewDeviceVerification = false)
        }
        composeTestRule.onNode(
            hasText(
                text = "We don't recognize this device",
                substring = true,
                ignoreCase = true,
            ),
        ).assertIsNotDisplayed()
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
    isRememberEnabled = false,
    isNewDeviceVerification = false,
    email = "example@email.com",
    password = "password123",
    orgIdentifier = "orgIdentifier",
)
