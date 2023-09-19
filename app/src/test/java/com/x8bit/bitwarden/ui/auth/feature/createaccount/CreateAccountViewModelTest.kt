package com.x8bit.bitwarden.ui.auth.feature.createaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
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
        )
        val handle = SavedStateHandle(mapOf("state" to savedState))
        val viewModel = CreateAccountViewModel(handle)
        assertEquals(savedState, viewModel.stateFlow.value)
    }

    @Test
    fun `SubmitClick should emit ShowToast`() = runTest {
        val viewModel = CreateAccountViewModel(SavedStateHandle())
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(SubmitClick)
            assert(awaitItem() is CreateAccountEvent.ShowToast)
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
        )
    }
}
