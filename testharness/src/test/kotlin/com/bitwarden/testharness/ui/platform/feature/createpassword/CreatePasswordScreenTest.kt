package com.bitwarden.testharness.ui.platform.feature.createpassword

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
 * Tests for the [CreatePasswordScreen] composable in the testharness module.
 *
 * Verifies that the Create Password screen displays all UI elements correctly
 * and handles user interactions appropriately following UDF patterns.
 */
class CreatePasswordScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false

    private val mutableEventFlow = MutableSharedFlow<CreatePasswordEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(
        CreatePasswordState(
            username = "",
            password = "",
            isLoading = false,
            resultText = "Ready to create password credential.\n\n" +
                "Enter username and password, then click Execute.",
        ),
    )
    private val viewModel = mockk<CreatePasswordViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            CreatePasswordScreen(
                onNavigateBack = { haveCalledOnNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `username field label is displayed`() {
        composeTestRule
            .onNodeWithText("Username")
            .assertIsDisplayed()
    }

    @Test
    fun `password field label is displayed`() {
        composeTestRule
            .onNodeWithText("Password")
            .assertIsDisplayed()
    }

    @Test
    fun `execute button label is displayed`() {
        composeTestRule
            .onNodeWithText("Execute")
            .assertIsDisplayed()
    }

    @Test
    fun `clear button label is displayed`() {
        composeTestRule
            .onNodeWithText("Clear")
            .assertIsDisplayed()
    }

    @Test
    fun `result field label is displayed`() {
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
            viewModel.trySendAction(CreatePasswordAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(CreatePasswordEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }
}
