package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateAccountViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct`() {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should pull from saved state handle when present`() {
        val savedState = CreateAccountState(
            emailInput = "email",
            passwordInput = "password",
            confirmPasswordInput = "confirmPassword",
            passwordHintInput = "hint",
            errorDialogState = BasicDialogState.Hidden,
        )
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = CreateAccountViewModel(handle)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SubmitClick with password below 12 chars should show password length dialog`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        val input = "abcdefghikl"
        viewModel.trySendAction(PasswordInputChange("abcdefghikl"))
        val expectedState = DEFAULT_STATE.copy(
            passwordInput = input,
            errorDialogState = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.master_password_length_val_message_x.asText(12),
            ),
        )
        viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `SubmitClick with long enough password emit ShowToast`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.trySendAction(PasswordInputChange("longenoughpassword"))
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CreateAccountAction.SubmitClick)
            assert(awaitItem() is CreateAccountEvent.ShowToast)
        }
    }

    @Test
    fun `CloseClick should emit NavigateBack`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(CloseClick)
            assert(awaitItem() is CreateAccountEvent.NavigateBack)
        }
    }

    @Test
    fun `ConfirmPasswordInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.actionChannel.trySend(ConfirmPasswordInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(confirmPasswordInput = "input"), awaitItem())
        }
    }

    @Test
    fun `EmailInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.actionChannel.trySend(EmailInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(emailInput = "input"), awaitItem())
        }
    }

    @Test
    fun `PasswordHintChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.actionChannel.trySend(PasswordHintChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordHintInput = "input"), awaitItem())
        }
    }

    @Test
    fun `PasswordInputChange update passwordInput`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.actionChannel.trySend(PasswordInputChange("input"))
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE.copy(passwordInput = "input"), awaitItem())
        }
    }

    companion object {
        private val DEFAULT_STATE = CreateAccountState(
            passwordInput = "",
            emailInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            errorDialogState = BasicDialogState.Hidden,
        )
    }
}
