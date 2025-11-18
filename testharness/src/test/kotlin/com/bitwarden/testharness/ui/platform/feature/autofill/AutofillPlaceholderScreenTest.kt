package com.bitwarden.testharness.ui.platform.feature.autofill

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * Tests for the [AutofillPlaceholderScreen] composable in the testharness module.
 *
 * Verifies that the placeholder screen displays correctly and handles back navigation
 * following UDF patterns.
 */
class AutofillPlaceholderScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false

    private val mutableEventFlow = MutableSharedFlow<AutofillPlaceholderEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(Unit)
    private val viewModel = mockk<AutofillPlaceholderViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            AutofillPlaceholderScreen(
                onNavigateBack = { haveCalledOnNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `placeholder text is displayed on screen`() {
        composeTestRule
            .onNodeWithText("Autofill testing functionality coming soon")
            .assertIsDisplayed()
    }

    @Test
    fun `back button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            viewModel.trySendAction(AutofillPlaceholderAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AutofillPlaceholderEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }
}
