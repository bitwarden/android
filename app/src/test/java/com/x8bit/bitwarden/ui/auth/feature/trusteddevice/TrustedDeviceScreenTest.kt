package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrustedDeviceScreenTest : BaseComposeTest() {

    private var onNavigateToAdminApprovalEmail: String? = null
    private var onNavigateToLoginWithOtherDeviceEmail: String? = null
    private var onNavigateToLockEmail: String? = null

    private val mutableEventFlow = bufferedMutableSharedFlow<TrustedDeviceEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<TrustedDeviceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            TrustedDeviceScreen(
                viewModel = viewModel,
                onNavigateToAdminApproval = { onNavigateToAdminApprovalEmail = it },
                onNavigateToLoginWithOtherDevice = { onNavigateToLoginWithOtherDeviceEmail = it },
                onNavigateToLock = { onNavigateToLockEmail = it },
            )
        }
    }

    @Test
    fun `on NavigateToApproveWithDevice event should invoke onNavigateToAdminApproval`() {
        val email = "test@bitwarden.com"
        mutableEventFlow.tryEmit(TrustedDeviceEvent.NavigateToApproveWithAdmin(email))
        assertEquals(onNavigateToAdminApprovalEmail, email)
    }

    @Test
    fun `on NavigateToApproveWithDevice event should invoke onNavigateToLoginWithOtherDevice`() {
        val email = "test@bitwarden.com"
        mutableEventFlow.tryEmit(TrustedDeviceEvent.NavigateToApproveWithDevice(email))
        assertEquals(onNavigateToLoginWithOtherDeviceEmail, email)
    }

    @Test
    fun `on NavigateToLockScreen event should invoke NavigateToLockScreen`() {
        val email = "test@bitwarden.com"
        mutableEventFlow.tryEmit(TrustedDeviceEvent.NavigateToLockScreen(email))
        assertEquals(onNavigateToLockEmail, email)
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.BackClick)
        }
    }

    @Test
    fun `on remember toggle changed should send RememberToggle`() {
        composeTestRule
            .onNodeWithText("Remember this device")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.RememberToggle(true))
        }
    }

    @Test
    fun `continue button should be displayed according to state`() {
        composeTestRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(showContinueButton = false) }

        composeTestRule
            .onNodeWithText("Continue")
            .assertDoesNotExist()
    }

    @Test
    fun `on continue clicked should send ContinueClick`() {
        composeTestRule
            .onNodeWithText("Continue")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.ContinueClick)
        }
    }

    @Test
    fun `other device button should be displayed according to state`() {
        composeTestRule
            .onNodeWithText("Approve with my other device")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(showOtherDeviceButton = false) }

        composeTestRule
            .onNodeWithText("Approve with my other device")
            .assertDoesNotExist()
    }

    @Test
    fun `on approve with device clicked should send ApproveWithDeviceClick`() {
        composeTestRule
            .onNodeWithText("Approve with my other device")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithDeviceClick)
        }
    }

    @Test
    fun `admin approval button should be displayed according to state`() {
        composeTestRule
            .onNodeWithText("Request admin approval")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(showRequestAdminButton = false) }

        composeTestRule
            .onNodeWithText("Request admin approval")
            .assertDoesNotExist()
    }

    @Test
    fun `on approve with admin clicked should send ApproveWithAdminClick`() {
        composeTestRule
            .onNodeWithText("Request admin approval")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithAdminClick)
        }
    }

    @Test
    fun `master password button should be displayed according to state`() {
        composeTestRule
            .onNodeWithText("Approve with master password")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(showMasterPasswordButton = false) }

        composeTestRule
            .onNodeWithText("Approve with master password")
            .assertDoesNotExist()
    }

    @Test
    fun `on approve with master password clicked should send ApproveWithPasswordClick`() {
        composeTestRule
            .onNodeWithText("Approve with master password")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithPasswordClick)
        }
    }

    @Test
    fun `on not you clicked should send NotYouClick`() {
        composeTestRule
            .onNodeWithText("Not you?")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(TrustedDeviceAction.NotYouClick)
        }
    }

    @Test
    fun `remember this device toggle should update according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithText("Remember this device")
            .performScrollTo()
            .assertIsOff()

        mutableStateFlow.update { DEFAULT_STATE.copy(isRemembered = true) }

        composeTestRule
            .onNodeWithText("Remember this device")
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `email and environment label should update according to state`() {
        mutableStateFlow.update { DEFAULT_STATE }

        composeTestRule
            .onNodeWithText("Logging in as bitwarden@email.com on vault.test.pw")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Logging in as email@bitwarden.com on vault.bitwarden.pw")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                emailAddress = "bitwarden@email.com",
                environmentLabel = "vault.test.pw",
            )
        }

        composeTestRule
            .onNodeWithText("Logging in as email@bitwarden.com on vault.bitwarden.pw")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Logging in as bitwarden@email.com on vault.test.pw")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `dialog should update according to state`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = TrustedDeviceState.DialogState.Loading(message = "Loading".asText()),
            )
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Loading")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                dialogState = TrustedDeviceState.DialogState.Error(
                    title = "Hello".asText(),
                    message = "World".asText(),
                ),
            )
        }
        composeTestRule.onNode(isDialog()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "Hello")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = "World")
            .assert(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(dialogState = null) }
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }
}

private val DEFAULT_STATE: TrustedDeviceState = TrustedDeviceState(
    dialogState = null,
    emailAddress = "email@bitwarden.com",
    environmentLabel = "vault.bitwarden.pw",
    isRemembered = false,
    showContinueButton = true,
    showOtherDeviceButton = true,
    showRequestAdminButton = true,
    showMasterPasswordButton = true,
)
