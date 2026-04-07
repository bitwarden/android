package com.bitwarden.testharness.ui.platform.feature.landing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the [LandingScreen] composable in the testharness module.
 *
 * Verifies that the Landing screen displays navigation options correctly
 * and handles user interactions appropriately following UDF patterns.
 */
class LandingScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateToAutofill = false
    private var haveCalledOnNavigateToCredentialManager = false

    private val mutableEventFlow = MutableSharedFlow<LandingEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(Unit)
    private val viewModel = mockk<LandingViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            LandingScreen(
                onNavigateToAutofill = { haveCalledOnNavigateToAutofill = true },
                onNavigateToCredentialManager = { haveCalledOnNavigateToCredentialManager = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `test categories header text is displayed`() {
        composeTestRule
            .onNodeWithText("TEST CATEGORIES")
            .assertIsDisplayed()
    }

    @Test
    fun `autofill button text is displayed`() {
        composeTestRule
            .onNodeWithText("Autofill")
            .assertIsDisplayed()
    }

    @Test
    fun `credential manager button text is displayed`() {
        composeTestRule
            .onNodeWithText("Credential Manager")
            .assertIsDisplayed()
    }

    @Test
    fun `autofill button click should send OnAutofillClick action`() {
        composeTestRule
            .onNodeWithText("Autofill")
            .performClick()

        verify { viewModel.trySendAction(LandingAction.OnAutofillClick) }
    }

    @Test
    fun `credential manager button click should send OnCredentialManagerClick action`() {
        composeTestRule
            .onNodeWithText("Credential Manager")
            .performClick()

        verify { viewModel.trySendAction(LandingAction.OnCredentialManagerClick) }
    }

    @Test
    fun `NavigateToAutofill event should call onNavigateToAutofill`() {
        mutableEventFlow.tryEmit(LandingEvent.NavigateToAutofill)

        assertTrue(haveCalledOnNavigateToAutofill)
    }

    @Test
    fun `NavigateToCredentialManager event should call onNavigateToCredentialManager`() {
        mutableEventFlow.tryEmit(LandingEvent.NavigateToCredentialManager)

        assertTrue(haveCalledOnNavigateToCredentialManager)
    }
}
