package com.bitwarden.testharness.ui.platform.feature.getpassword

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
 * Tests for the [GetPasswordScreen] composable in the testharness module.
 *
 * Verifies that the Get Password screen displays text and controls correctly
 * and handles user interactions appropriately following UDF patterns.
 */
class GetPasswordScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false

    private val mutableEventFlow = MutableSharedFlow<GetPasswordEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(
        GetPasswordState(
            isLoading = false,
            resultText = "Ready to retrieve password.\n\n" +
                "Click Execute to open the password picker.",
        ),
    )
    private val viewModel = mockk<GetPasswordViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            GetPasswordScreen(
                onNavigateBack = { haveCalledOnNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `helper text is displayed`() {
        composeTestRule
            .onNodeWithText("No inputs required. Click Execute to open credential picker.")
            .assertIsDisplayed()
    }

    @Test
    fun `execute button is displayed`() {
        composeTestRule
            .onNodeWithText("Execute")
            .assertIsDisplayed()
    }

    @Test
    fun `clear button is displayed`() {
        composeTestRule
            .onNodeWithText("Clear")
            .assertIsDisplayed()
    }

    @Test
    fun `result field is displayed`() {
        composeTestRule
            .onNodeWithText("Result")
            .assertIsDisplayed()
    }

    @Test
    fun `back button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            viewModel.trySendAction(GetPasswordAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(GetPasswordEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }
}
