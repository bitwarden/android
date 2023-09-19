package com.x8bit.bitwarden.ui.auth.feature.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginViewModelTest : BaseViewModelTest() {

    private val savedStateHandle = SavedStateHandle().also {
        it["email_address"] = "test@gmail.com"
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = LoginViewModel(
            authRepository = mockk(),
            savedStateHandle = savedStateHandle,
        )
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            passwordInput = "input",
            isLoginButtonEnabled = true,
        )
        val handle = SavedStateHandle(
            mapOf(
                "email_address" to "test@gmail.com",
                "state" to expectedState,
            ),
        )
        val viewModel = LoginViewModel(
            authRepository = mockk(),
            savedStateHandle = handle,
        )
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `LoginButtonClick login returns error should do nothing`() = runTest {
        // TODO: handle and display errors (BIT-320)
        val authRepository = mockk<AuthRepository> {
            coEvery { login(email = "test@gmail.com", password = "") } returns LoginResult.Error
        }
        val viewModel = LoginViewModel(
            authRepository = authRepository,
            savedStateHandle = savedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.LoginButtonClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        }
        coVerify {
            authRepository.login(email = "test@gmail.com", password = "")
        }
    }

    @Test
    fun `LoginButtonClick login returns success should do nothing`() = runTest {
        val authRepository = mockk<AuthRepository> {
            coEvery { login("test@gmail.com", "") } returns LoginResult.Success
        }
        val viewModel = LoginViewModel(
            authRepository = authRepository,
            savedStateHandle = savedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.LoginButtonClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        }
        coVerify {
            authRepository.login(email = "test@gmail.com", password = "")
        }
    }

    @Test
    fun `SingleSignOnClick should do nothing`() = runTest {
        val viewModel = LoginViewModel(
            authRepository = mockk(),
            savedStateHandle = savedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.SingleSignOnClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        }
    }

    @Test
    fun `NotYouButtonClick should emit NavigateToLanding`() = runTest {
        val viewModel = LoginViewModel(
            authRepository = mockk(),
            savedStateHandle = savedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.NotYouButtonClick)
            assertEquals(
                LoginEvent.NavigateToLanding,
                awaitItem(),
            )
        }
    }

    @Test
    fun `PasswordInputChanged should update password input`() = runTest {
        val input = "input"
        val viewModel = LoginViewModel(
            authRepository = mockk(),
            savedStateHandle = savedStateHandle,
        )
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginAction.PasswordInputChanged(input))
            assertEquals(
                DEFAULT_STATE.copy(passwordInput = input),
                viewModel.stateFlow.value,
            )
        }
    }

    companion object {
        private val DEFAULT_STATE = LoginState(
            emailAddress = "test@gmail.com",
            passwordInput = "",
            isLoginButtonEnabled = true,
        )
    }
}
