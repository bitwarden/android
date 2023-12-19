package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class PasswordHistoryScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableEventFlow = MutableSharedFlow<PasswordHistoryEvent>(
        extraBufferCapacity = Int.MAX_VALUE,
    )

    private val mutableStateFlow = MutableStateFlow(
        PasswordHistoryState(PasswordHistoryState.ViewState.Loading),
    )

    private val viewModel = mockk<PasswordHistoryViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            PasswordHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { onNavigateBackCalled = true },
            )
        }
    }

    @Test
    fun `Empty state should display no passwords message`() {
        updateState(PasswordHistoryState(PasswordHistoryState.ViewState.Empty))
        composeTestRule.onNodeWithText("No passwords to list.").assertIsDisplayed()
    }

    @Test
    fun `Error state should display error message`() {
        val errorMessage = "Error occurred"
        updateState(
            PasswordHistoryState(PasswordHistoryState.ViewState.Error(errorMessage.asText())),
        )
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `navigation icon click should trigger navigate back`() {
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        verify {
            viewModel.trySendAction(
                PasswordHistoryAction.CloseClick,
            )
        }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PasswordHistoryEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `clicking the Copy button should send PasswordCopyClick action`() {
        val password = PasswordHistoryState.GeneratedPassword(password = "Password", date = "Date")
        updateState(
            PasswordHistoryState(
                PasswordHistoryState.ViewState.Content(
                    passwords = listOf(password),
                ),
            ),
        )

        composeTestRule.onNodeWithText(password.password).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Copy").performClick()

        verify {
            viewModel.trySendAction(
                PasswordHistoryAction.PasswordCopyClick(
                    PasswordHistoryState.GeneratedPassword("Password", "Date"),
                ),
            )
        }
    }

    @Test
    fun `clicking the Clear button in the overflow menu should send PasswordClearClick action`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onNodeWithText("Clear")
            .performClick()

        verify {
            viewModel.trySendAction(PasswordHistoryAction.PasswordClearClick)
        }
    }

    @Test
    fun `Content state should display list of passwords`() {
        val passwords =
            listOf(PasswordHistoryState.GeneratedPassword(password = "Password1", date = "Date1"))

        updateState(
            PasswordHistoryState(
                PasswordHistoryState.ViewState.Content(
                    passwords = passwords,
                ),
            ),
        )

        composeTestRule.onNodeWithText("Password1").assertIsDisplayed()
    }

    private fun updateState(state: PasswordHistoryState) {
        mutableStateFlow.value = state
    }
}
