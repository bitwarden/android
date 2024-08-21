package com.x8bit.bitwarden.ui.auth.feature.removepassword

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.util.assertNoDialogExists
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class RemovePasswordScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<RemovePasswordViewModel>(relaxed = true) {
        every { eventFlow } returns bufferedMutableSharedFlow()
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            RemovePasswordScreen(
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `dialog should update according to state`() {
        val errorTitle = "message title"
        val errorMessage = "Error message"
        composeTestRule.assertNoDialogExists()
        composeTestRule.onNodeWithText(text = errorTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(text = errorMessage).assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Error(
                    title = errorTitle.asText(),
                    message = errorMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = errorTitle)
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()
        composeTestRule
            .onNodeWithText(text = errorMessage)
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()

        val loadingMessage = "Loading message"
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Loading(
                    title = loadingMessage.asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText(text = loadingMessage)
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()

        mutableStateFlow.update { it.copy(dialogState = null) }

        composeTestRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `description should update according to state`() {
        val description = "description"
        composeTestRule.onNodeWithText(text = description).assertDoesNotExist()

        mutableStateFlow.update { it.copy(description = description.asText()) }

        composeTestRule.onNodeWithText(text = description).isDisplayed()
    }

    @Test
    fun `continue button should update according to state`() {
        composeTestRule.onNodeWithText(text = "Continue").performScrollTo().assertIsNotEnabled()
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule.onNodeWithText(text = "Continue").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `continue button click should emit ContinueClick`() {
        mutableStateFlow.update { it.copy(input = "a") }
        composeTestRule
            .onNodeWithText(text = "Continue")
            .performScrollTo()
            .performClick()

        verify(exactly = 1) {
            viewModel.trySendAction(RemovePasswordAction.ContinueClick)
        }
    }
}

private val DEFAULT_STATE = RemovePasswordState(
    input = "",
    dialogState = null,
    description = "My org".asText(),
)
