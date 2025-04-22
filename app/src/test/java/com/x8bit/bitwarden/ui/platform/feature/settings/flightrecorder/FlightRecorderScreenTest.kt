package com.x8bit.bitwarden.ui.platform.feature.settings.flightrecorder

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.repository.model.FlightRecorderDuration
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.util.performCustomAccessibilityAction
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlightRecorderScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<FlightRecorderEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel = mockk<FlightRecorderViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    private val intentManager = mockk<IntentManager> {
        every { launchUri(uri = any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
        ) {
            FlightRecorderScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on navigation icon click should emit BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Close")
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(FlightRecorderAction.BackClick)
        }
    }

    @Test
    fun `on save click should emit SaveClick action`() {
        composeTestRule.onNodeWithText(text = "Save").performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(FlightRecorderAction.SaveClick)
        }
    }

    @Test
    fun `on help center click should emit HelpCenterClick action`() {
        composeTestRule
            .onNodeWithText(text = "Bitwarden help center", substring = true)
            .performScrollTo()
            .performCustomAccessibilityAction(label = "Bitwarden help center")
        verify(exactly = 1) {
            viewModel.trySendAction(FlightRecorderAction.HelpCenterClick)
        }
    }

    @Test
    fun `on logging duration click should display select dialog`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithContentDescription(label = "1 hour. Logging duration")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText(text = "Logging duration")
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()
    }

    @Test
    fun `on selection of new duration should emit action`() {
        composeTestRule.assertNoDialogExists()
        composeTestRule
            .onNodeWithContentDescription(label = "1 hour. Logging duration")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText(text = "24 hours")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(
                FlightRecorderAction.DurationSelect(FlightRecorderDuration.TWENTY_FOUR_HOURS),
            )
        }
    }

    @Test
    fun `on NavigateBack event should invoke onNavigateBack`() {
        mutableEventFlow.tryEmit(FlightRecorderEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on NavigateToHelpCenter event should launch intent for help center`() {
        mutableEventFlow.tryEmit(FlightRecorderEvent.NavigateToHelpCenter)
        verify(exactly = 1) {
            intentManager.launchUri(uri = "https://bitwarden.com/help/flight-recorder".toUri())
        }
    }
}

private val DEFAULT_STATE: FlightRecorderState =
    FlightRecorderState(
        selectedDuration = FlightRecorderDuration.ONE_HOUR,
    )
