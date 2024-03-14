package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class TrustedDeviceScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled: Boolean = false

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
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(TrustedDeviceEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
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
}

private val DEFAULT_STATE: TrustedDeviceState = TrustedDeviceState(
    emailAddress = "email@bitwarden.com",
    environmentLabel = "vault.bitwarden.pw",
    isRemembered = false,
)
