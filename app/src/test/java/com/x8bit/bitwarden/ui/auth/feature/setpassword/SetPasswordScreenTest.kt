package com.x8bit.bitwarden.ui.auth.feature.setpassword

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

class SetPasswordScreenTest : BaseComposeTest() {
    private val mutableEventFlow = bufferedMutableSharedFlow<SetPasswordEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    val viewModel = mockk<SetPasswordViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            SetPasswordScreen(
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `basicDialog should update according to state`() {
        composeTestRule
            .onNodeWithText("Error message")
            .assertDoesNotExist()
        composeTestRule.assertNoDialogExists()

        mutableStateFlow.update {
            it.copy(
                dialogState = SetPasswordState.DialogState.Error(
                    title = null,
                    message = "Error message".asText(),
                ),
            )
        }

        composeTestRule
            .onNodeWithText("Error message")
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()
    }

    @Test
    fun `loadingDialog should update according to state`() {
        composeTestRule.onNodeWithText("Loading...").assertDoesNotExist()

        mutableStateFlow.update {
            it.copy(
                dialogState = SetPasswordState.DialogState.Loading(
                    message = "Loading...".asText(),
                ),
            )
        }

        composeTestRule.onNodeWithText("Loading...").isDisplayed()
    }

    @Test
    fun `cancel button click should emit CancelClick`() {
        composeTestRule.onNodeWithText("Cancel").performClick()

        verify {
            viewModel.trySendAction(SetPasswordAction.CancelClick)
        }
    }

    @Test
    fun `submit button click should emit SubmitClick`() {
        composeTestRule.onNodeWithText("Submit").performClick()

        verify {
            viewModel.trySendAction(SetPasswordAction.SubmitClick)
        }
    }

    @Test
    fun `password input change should send PasswordInputChange action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Master password").performTextInput(input)
        verify {
            viewModel.trySendAction(SetPasswordAction.PasswordInputChanged("Test123"))
        }
    }

    @Test
    fun `retype password input change should send RetypePasswordInputChanged action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Re-type master password").performTextInput(input)
        verify {
            viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged("Test123"))
        }
    }

    @Test
    fun `password hint input change should send PasswordHintInputChanged action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Master password hint (optional)").performTextInput(input)
        verify {
            viewModel.trySendAction(SetPasswordAction.PasswordHintInputChanged("Test123"))
        }
    }

    @Test
    fun `toggling one password field visibility should toggle the other`() {
        // should start with 2 Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)[0]
            .performClick()

        // after clicking there should be no Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(0)

        // and there should be 2 hide buttons now, and we'll click the second one:
        composeTestRule
            .onAllNodesWithContentDescription("Hide")
            .assertCountEquals(2)[1]
            .performClick()

        // then there should be two show buttons again
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(2)
    }
}

private val DEFAULT_STATE = SetPasswordState(
    dialogState = null,
    organizationIdentifier = "SSO",
    passwordHintInput = "",
    passwordInput = "",
    policies = emptyList(),
    retypePasswordInput = "",
)
