package com.bitwarden.authenticator.ui.authenticator.feature.tutorial

import app.cash.turbine.test
import com.bitwarden.authenticator.ui.platform.base.BaseViewModelTest
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialAction
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialEvent
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialState
import com.bitwarden.authenticator.ui.platform.feature.tutorial.TutorialViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TutorialViewModelTest : BaseViewModelTest() {
    private lateinit var viewModel: TutorialViewModel

    @BeforeEach
    fun setUp() {
        viewModel = TutorialViewModel()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
        }
    }

    @Test
    fun `PagerSwipe should update state`() = runTest {
        val newIndex = 2
        viewModel.trySendAction(TutorialAction.PagerSwipe(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Test
    fun `DotClick should update state and emit UpdatePager`() = runTest {
        val newIndex = 2

        viewModel.trySendAction(TutorialAction.DotClick(index = newIndex))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = newIndex),
                awaitItem(),
            )
        }
        viewModel.eventFlow.test {
            assertEquals(
                TutorialEvent.UpdatePager(index = newIndex),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ContinueClick should emit NavigateToAuthenticator when at the end of pages`() = runTest {
        // Step 1: Verify state updates for index 0 -> 1
        viewModel.trySendAction(TutorialAction.ContinueClick(0))
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = 1),
                awaitItem(),
            )
        }

        // Step 2: Verify state updates for index 1 -> 2
        viewModel.trySendAction(TutorialAction.ContinueClick(1))
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(index = 2),
                awaitItem(),
            )
        }
        // Step 3: Clean up any residual events before asserting event emission
        viewModel.eventFlow.test {
            cancelAndConsumeRemainingEvents() // Clear all old events
        }

        // Step 4: Verify event emission when reaching the end of the pages
        viewModel.trySendAction(TutorialAction.ContinueClick(2))
        viewModel.eventFlow.test {
            assertEquals(
                TutorialEvent.NavigateToAuthenticator,
                awaitItem(),
            )
        }
    }

    @Test
    fun `SkipClick should emit NavigateToAuthenticator`() = runTest {
        viewModel.trySendAction(TutorialAction.SkipClick)

        viewModel.eventFlow.test {
            assertEquals(
                TutorialEvent.NavigateToAuthenticator,
                awaitItem(),
            )
        }
    }
}

private val DEFAULT_STATE = TutorialState(
    index = 0,
    pages = listOf(
        TutorialState.TutorialSlide.IntroSlide,
        TutorialState.TutorialSlide.QrScannerSlide,
        TutorialState.TutorialSlide.UniqueCodesSlide,
    ),
)
