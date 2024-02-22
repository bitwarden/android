package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class PasswordHistoryScreenTest : BaseComposeTest() {
    private var onNavigateBackCalled = false

    private val mutableEventFlow = bufferedMutableSharedFlow<PasswordHistoryEvent>()

    private val mutableStateFlow = MutableStateFlow(
        PasswordHistoryState(
            passwordHistoryMode = GeneratorPasswordHistoryMode.Default,
            viewState = PasswordHistoryState.ViewState.Loading,
        ),
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
        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHistoryState.ViewState.Empty,
            )
        }
        composeTestRule.onNodeWithText("No passwords to list.").assertIsDisplayed()
    }

    @Test
    fun `Error state should display error message`() {
        val errorMessage = "Error occurred"
        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHistoryState.ViewState.Error(errorMessage.asText()),
            )
        }
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
        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHistoryState.ViewState.Content(
                    passwords = listOf(password),
                ),
            )
        }

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
    fun `Clear button should depend on state`() {
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .performClick()

        composeTestRule
            .onAllNodesWithText("Clear")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                passwordHistoryMode = GeneratorPasswordHistoryMode.Item(itemId = "mockId-1"),
            )
        }
        composeTestRule
            .onNodeWithContentDescription(label = "More")
            .assertIsNotDisplayed()

        composeTestRule
            .onAllNodesWithText("Clear")
            .filterToOne(hasAnyAncestor(isPopup()))
            .assertIsNotDisplayed()
    }

    @Test
    fun `Content state should display list of passwords`() {
        val passwords =
            listOf(PasswordHistoryState.GeneratedPassword(password = "Password1", date = "Date1"))

        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHistoryState.ViewState.Content(
                    passwords = passwords,
                ),
            )
        }

        composeTestRule.onNodeWithText("Password1").assertIsDisplayed()
    }
}
