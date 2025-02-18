package com.bitwarden.authenticator.ui.authenticator.feature.tutorial

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialAction
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialEvent
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialScreen
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialState
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class TutorialScreenTest : BaseComposeTest() {
    private var onTutorialFinishedCalled = false
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<TutorialEvent>()
    private val viewModel = mockk<TutorialViewModel>(relaxed = true) {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            TutorialScreen(
                viewModel = viewModel,
                onTutorialFinished = { onTutorialFinishedCalled = true },
            )
        }
    }

    @Test
    fun `pages should display and update according to state`() {
        composeTestRule
            .onNodeWithText("Secure your accounts with Bitwarden Authenticator")
            .assertExists()
            .assertIsDisplayed()

        mutableEventFlow.tryEmit(TutorialEvent.UpdatePager(index = 1))
        composeTestRule
            .onNodeWithText("Secure your accounts with Bitwarden Authenticator")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Use your device camera to scan codes")
            .assertExists()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(pages = listOf(TutorialState.TutorialSlide.UniqueCodesSlide))
        }
        composeTestRule
            .onNodeWithText("Sign in using unique codes")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `Primary action button should say Continue when not at the end of the slides`() {
        composeTestRule
            .onNodeWithText("Continue")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `Primary action button should say Get started when at the end of the slides`() {
        mutableStateFlow.update {
            it.copy(pages = listOf(TutorialState.TutorialSlide.UniqueCodesSlide))
        }
        composeTestRule
            .onNodeWithText("Get Started")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `NavigateToAuthenticator event should call onTutorialFinished`() {
        mutableEventFlow.tryEmit(TutorialEvent.NavigateToAuthenticator)
        assertTrue(onTutorialFinishedCalled)
    }

    @Test
    fun `continue button click should send ContinueClick action`() {
        composeTestRule
            .onNodeWithText("Continue")
            .performClick()
        verify {
            viewModel.trySendAction(TutorialAction.ContinueClick(mutableStateFlow.value.index))
        }
    }

    @Test
    fun `get started button click should send ContinueClick action`() {
        mutableStateFlow.update {
            it.copy(pages = listOf(TutorialState.TutorialSlide.UniqueCodesSlide))
        }
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        verify {
            viewModel.trySendAction(TutorialAction.ContinueClick(mutableStateFlow.value.index))
        }
    }

    @Test
    fun `skip button click should send SkipClick action`() {
        composeTestRule
            .onNodeWithText("Skip")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(TutorialAction.SkipClick) }
    }
}

private val DEFAULT_STATE = TutorialState(
    index = 0,
    pages = listOf(
        TutorialState.TutorialSlide.IntroSlide,
        TutorialState.TutorialSlide.QrScannerSlide,
    ),
)
