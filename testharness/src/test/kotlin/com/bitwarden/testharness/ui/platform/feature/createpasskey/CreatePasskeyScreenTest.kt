package com.bitwarden.testharness.ui.platform.feature.createpasskey

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
 * Tests for the [CreatePasskeyScreen] composable in the testharness module.
 *
 * Verifies that the Create Passkey screen displays input fields, buttons, and result area
 * correctly and handles user interactions appropriately following UDF patterns.
 */
class CreatePasskeyScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false

    private val mutableEventFlow = MutableSharedFlow<CreatePasskeyEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(
        CreatePasskeyState(
            username = "",
            rpId = "",
            origin = "",
            isLoading = false,
            resultText = "Ready to create passkey credential.\n\n" +
                "Enter username and Relying Party ID, then click Execute.",
        ),
    )
    private val viewModel = mockk<CreatePasskeyViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            CreatePasskeyScreen(
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
    fun `relying party id field label is displayed`() {
        composeTestRule
            .onNodeWithText("Relying Party ID")
            .assertIsDisplayed()
    }

    @Test
    fun `origin optional field label is displayed`() {
        composeTestRule
            .onNodeWithText("Origin (optional)")
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
            .assertExists()
    }

    @Test
    fun `back button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            viewModel.trySendAction(CreatePasskeyAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(CreatePasskeyEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }
}
