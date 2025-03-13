package com.x8bit.bitwarden.ui.auth.feature.checkemail

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckEmailScreenTest : BaseComposeTest() {
    private val intentManager = mockk<IntentManager>(relaxed = true) {
        every { startDefaultEmailApplication() } just runs
    }
    private var onNavigateBackCalled = false

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<CheckEmailEvent>()
    private val viewModel = mockk<CheckEmailViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
        ) {
            CheckEmailScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `close button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify {
            viewModel.trySendAction(CheckEmailAction.BackClick)
        }
    }

    @Test
    fun `open email app button click should send OpenEmailClcik action`() {
        composeTestRule
            .onNodeWithText("Open email app")
            .performScrollTo()
            .performClick()

        verify {
            viewModel.trySendAction(CheckEmailAction.OpenEmailClick)
        }
    }

    @Test
    fun `NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(CheckEmailEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `NavigateToEmailApp should call openEmailApp`() {
        mutableEventFlow.tryEmit(CheckEmailEvent.NavigateToEmailApp)
        verify {
            intentManager.startDefaultEmailApplication()
        }
    }

    @Test
    fun `change email button click should send ChangeEmailClick action`() {
        composeTestRule
            .onNodeWithText("Change email address")
            .performScrollTo()
            .performClick()

        verify { viewModel.trySendAction(CheckEmailAction.ChangeEmailClick) }
    }

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = CheckEmailState(
            email = EMAIL,
        )
    }
}
