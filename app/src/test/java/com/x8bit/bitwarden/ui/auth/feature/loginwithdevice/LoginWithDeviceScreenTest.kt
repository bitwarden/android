package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.isProgressBar
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class LoginWithDeviceScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<LoginWithDeviceEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<LoginWithDeviceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            LoginWithDeviceScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `close button click should send CloseButtonClick action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(LoginWithDeviceAction.CloseButtonClick)
        }
    }

    @Test
    fun `resend notification click should send ResendNotificationClick action`() {
        composeTestRule.onNodeWithText("Resend notification").performClick()
        verify {
            viewModel.trySendAction(LoginWithDeviceAction.ResendNotificationClick)
        }
    }

    @Test
    fun `view all log in options click should send ViewAllLogInOptionsClick action`() {
        composeTestRule.onNodeWithText("View all log in options").performScrollTo().performClick()
        verify {
            viewModel.trySendAction(LoginWithDeviceAction.ViewAllLogInOptionsClick)
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(LoginWithDeviceEvent.NavigateBack)
        TestCase.assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `progress bar should be displayed according to state`() {
        mutableStateFlow.update {
            it.copy(viewState = LoginWithDeviceState.ViewState.Loading)
        }
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = LoginWithDeviceState.ViewState.Error("Failure".asText()))
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_STATE.viewState)
        }
        composeTestRule.onNode(isProgressBar).assertDoesNotExist()
    }

    @Test
    fun `error should be displayed according to state`() {
        val errorMessage = "error"
        mutableStateFlow.update {
            it.copy(viewState = LoginWithDeviceState.ViewState.Loading)
        }
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(viewState = LoginWithDeviceState.ViewState.Error(errorMessage.asText()))
        }
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(viewState = DEFAULT_STATE.viewState)
        }
        composeTestRule.onNodeWithText(errorMessage).assertDoesNotExist()
    }

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = LoginWithDeviceState(
            emailAddress = EMAIL,
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = "alabster-drinkable-mystified-rapping-irrigate",
            ),
        )
    }
}
