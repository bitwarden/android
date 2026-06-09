package com.x8bit.bitwarden.ui.platform.feature.accessibilitydisclosure

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.exit.ExitManager
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibilityDisclosureScreenTest : BitwardenComposeTest() {

    private var onDismissCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<AccessibilityDisclosureEvent>()
    private val mutableStateFlow = MutableStateFlow(AccessibilityDisclosureState)
    private val viewModel = mockk<AccessibilityDisclosureViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val exitManager = mockk<ExitManager> {
        every { exitApplication() } just runs
    }

    @Before
    fun setUp() {
        setContent(exitManager = exitManager) {
            AccessibilityDisclosureScreen(
                onDismiss = { onDismissCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `accept button click should send AcceptClicked action`() {
        composeTestRule
            .onNodeWithText(text = "Accept")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AccessibilityDisclosureAction.AcceptClicked)
        }
    }

    @Test
    fun `close app button click should send CloseAppClick action`() {
        composeTestRule
            .onNodeWithText(text = "Close app")
            .performScrollTo()
            .performClick()
        verify(exactly = 1) {
            viewModel.trySendAction(AccessibilityDisclosureAction.CloseAppClick)
        }
    }

    @Test
    fun `Dismiss event should call onDismiss`() {
        mutableEventFlow.tryEmit(AccessibilityDisclosureEvent.Dismiss)
        assertTrue(onDismissCalled)
    }

    @Test
    fun `CloseApp event should exit the application`() {
        mutableEventFlow.tryEmit(AccessibilityDisclosureEvent.CloseApp)
        verify(exactly = 1) {
            exitManager.exitApplication()
        }
    }

    @Test
    fun `system back should not dismiss the screen`() {
        backDispatcher?.onBackPressed()
        verify(exactly = 1) {
            viewModel.trySendAction(AccessibilityDisclosureAction.CloseAppClick)
        }
    }
}
