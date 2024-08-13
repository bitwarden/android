package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

class CheckEmailScreenTest : BaseComposeTest() {
    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startDefaultEmailApplication() } just runs
    }
    private var onNavigateBackCalled = false
    private var onNavigateBackToLandingCalled = false
    private var onNavigateToEmailAppCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<CheckEmailEvent>()
    private val viewModel = mockk<CheckEmailViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            CheckEmailScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateBackToLanding = { onNavigateBackToLandingCalled = true },
                viewModel = viewModel,
                intentManager = intentManager,
            )
        }
    }

    @Test
    fun `close button click should send CloseTap action`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        verify {
            viewModel.trySendAction(CheckEmailAction.CloseClick)
        }
    }

    @Test
    fun `open email app button click should send OpenEmailTap action`() {
        composeTestRule.onNodeWithText("Open email app").performClick()
        verify {
            viewModel.trySendAction(CheckEmailAction.OpenEmailClick)
        }
    }

    @Test
    fun `login button click should send LoginTap action`() {
        mutableEventFlow.tryEmit(CheckEmailEvent.NavigateBackToLanding)
        TestCase.assertTrue(onNavigateBackToLandingCalled)
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(CheckEmailEvent.NavigateBack)
        TestCase.assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToEmailApp should call openEmailApp`() {
        mutableEventFlow.tryEmit(CheckEmailEvent.NavigateToEmailApp)
        verify {
            intentManager.startDefaultEmailApplication()
        }
    }

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = CheckEmailState(
            email = EMAIL,
        )
    }
}
