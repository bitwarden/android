package com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey

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
 * Tests for the [GetPasswordOrPasskeyScreen] composable in the testharness module.
 *
 * Verifies that the Get Password or Passkey screen displays UI elements correctly
 * and handles user interactions appropriately following UDF patterns.
 */
class GetPasswordOrPasskeyScreenTest : BaseComposeTest() {
    private var haveCalledOnNavigateBack = false

    private val mutableEventFlow = MutableSharedFlow<GetPasswordOrPasskeyEvent>(replay = 1)
    private val mutableStateFlow = MutableStateFlow(
        GetPasswordOrPasskeyState(
            rpId = "",
            origin = "",
            isLoading = false,
            resultText = "Ready to retrieve password or passkey.\n\n" +
                "Enter Relying Party ID, then click Execute.\n" +
                "System picker will show both passwords and passkeys.",
        ),
    )
    private val viewModel = mockk<GetPasswordOrPasskeyViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setTestContent {
            GetPasswordOrPasskeyScreen(
                onNavigateBack = { haveCalledOnNavigateBack = true },
                viewModel = viewModel,
            )
        }
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
            .assertIsDisplayed()
    }

    @Test
    fun `back button click should send BackClick action`() {
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify {
            viewModel.trySendAction(GetPasswordOrPasskeyAction.BackClick)
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(GetPasswordOrPasskeyEvent.NavigateBack)

        assertTrue(haveCalledOnNavigateBack)
    }
}
