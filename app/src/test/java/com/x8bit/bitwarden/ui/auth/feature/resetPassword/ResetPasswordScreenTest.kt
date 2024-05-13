package com.x8bit.bitwarden.ui.auth.feature.resetPassword

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordAction
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordEvent
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordScreen
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordState
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordViewModel
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

class ResetPasswordScreenTest : BaseComposeTest() {
    private val mutableEventFlow = bufferedMutableSharedFlow<ResetPasswordEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<ResetPasswordViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ResetPasswordScreen(
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
                dialogState = ResetPasswordState.DialogState.Error(
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
                dialogState = ResetPasswordState.DialogState.Loading(
                    message = "Loading...".asText(),
                ),
            )
        }

        composeTestRule.onNodeWithText("Loading...").isDisplayed()
    }

    @Test
    fun `logout button click should display confirmation dialog and emit ConfirmLogoutClick`() {
        composeTestRule.onNodeWithText("Log out").performClick()

        composeTestRule
            .onNodeWithText("Are you sure you want to log out?")
            .assert(hasAnyAncestor(isDialog()))
            .isDisplayed()

        composeTestRule
            .onNodeWithText("Yes")
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        composeTestRule.assertNoDialogExists()
        verify {
            viewModel.trySendAction(ResetPasswordAction.ConfirmLogoutClick)
        }
    }

    @Test
    fun `submit button click should emit SubmitClick`() {
        composeTestRule.onNodeWithText("Submit").performClick()

        verify {
            viewModel.trySendAction(ResetPasswordAction.SubmitClick)
        }
    }

    @Test
    fun `instructions text should update according to state`() {
        val weakPasswordString = "Your master password does not meet one or more " +
            "of your organization policies. In order to access the vault, you must " +
            "update your master password now. Proceeding will log you out of your " +
            "current session, requiring you to log back in. Active sessions on other " +
            "devices may continue to remain active for up to one hour."
        composeTestRule
            .onNodeWithText(weakPasswordString)
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
            )
        }

        val adminChangeString =
            "Your master password was recently changed by an administrator in " +
                "your organization. In order to access the vault, you must update your master " +
                "password now. Proceeding will log you out of your current session, " +
                "requiring you to log back in. Active sessions on other devices may continue " +
                "to remain active for up to one hour."
        composeTestRule
            .onNodeWithText(adminChangeString)
            .assertIsDisplayed()
    }

    @Test
    fun `detailed instructions should update according to state`() {
        val baseString = "One or more organization policies require your master password to " +
            "meet the following requirements:"
        composeTestRule
            .onNodeWithText(baseString)
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                policies = listOf("Make password better".asText()),
            )
        }

        val updatedString = listOf(
            baseString,
            "Make password better",
        )
            .joinToString("\n  •  ")
        composeTestRule
            .onNodeWithText(updatedString)
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
            )
        }

        composeTestRule
            .onNodeWithText(baseString)
            .assertDoesNotExist()
    }

    @Test
    fun `current password input change should send CurrentPasswordInputChanged action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Current master password").performTextInput(input)
        verify {
            viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged("Test123"))
        }
    }

    @Test
    fun `current password field should update according to state`() {
        composeTestRule
            .onNodeWithText("Current master password")
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(
                resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
            )
        }

        composeTestRule
            .onNodeWithText("Current master password")
            .assertDoesNotExist()
    }

    @Test
    fun `password input change should send PasswordInputChange action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Master password").performTextInput(input)
        verify {
            viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged("Test123"))
        }
    }

    @Test
    fun `retype password input change should send RetypePasswordInputChanged action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Re-type master password").performTextInput(input)
        verify {
            viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged("Test123"))
        }
    }

    @Test
    fun `password hint input change should send PasswordHintInputChanged action`() {
        val input = "Test123"
        composeTestRule.onNodeWithText("Master password hint (optional)").performTextInput(input)
        verify {
            viewModel.trySendAction(ResetPasswordAction.PasswordHintInputChanged("Test123"))
        }
    }

    @Test
    fun `toggling one password field visibility should toggle the other`() {
        // should start with 3 Show buttons:
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(3)[1]
            .performClick()

        // after clicking, only the "previous master password" field in the "Show" state
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(1)

        // and there should be 2 hide buttons now, and we'll click the second one:
        composeTestRule
            .onAllNodesWithContentDescription("Hide")
            .assertCountEquals(2)[0]
            .performClick()

        // then there should be all three show buttons again
        composeTestRule
            .onAllNodesWithContentDescription("Show")
            .assertCountEquals(3)
    }
}

private val DEFAULT_STATE = ResetPasswordState(
    policies = emptyList(),
    resetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
    dialogState = null,
    currentPasswordInput = "",
    passwordInput = "",
    retypePasswordInput = "",
    passwordHintInput = "",
)
