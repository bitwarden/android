package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginWithDeviceViewModelTest : BaseViewModelTest() {

    private val authRepository = mockk<AuthRepository> {
        coEvery {
            getFingerprintPhrase(EMAIL)
        } returns UserFingerprintResult.Success("initialFingerprint")
        coEvery {
            createAuthRequest(EMAIL)
        } returns mockk<AuthRequestResult.Success>()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        coVerify { authRepository.createAuthRequest(EMAIL) }
        coVerify { authRepository.getFingerprintPhrase(EMAIL) }
    }

    @Test
    fun `initial state should be correct when set`() = runTest {
        val newEmail = "newEmail@gmail.com"

        coEvery {
            authRepository.createAuthRequest(newEmail)
        } returns mockk<AuthRequestResult.Success>()
        coEvery {
            authRepository.getFingerprintPhrase(newEmail)
        } returns UserFingerprintResult.Success("initialFingerprint")
        val state = LoginWithDeviceState(
            emailAddress = newEmail,
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = "initialFingerprint",
            ),
        )
        val viewModel = createViewModel(state)
        viewModel.stateFlow.test {
            assertEquals(state, awaitItem())
        }
        coVerify {
            authRepository.createAuthRequest(newEmail)
            authRepository.getFingerprintPhrase(newEmail)
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginWithDeviceAction.CloseButtonClick)
            assertEquals(
                LoginWithDeviceEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `ResendNotificationClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginWithDeviceAction.ResendNotificationClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginWithDeviceEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ViewAllLogInOptionsClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(LoginWithDeviceAction.ViewAllLogInOptionsClick)
            assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
            assertEquals(
                LoginWithDeviceEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `on fingerprint result success received should show content`() = runTest {
        val newFingerprint = "newFingerprint"
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        viewModel.actionChannel.trySend(
            LoginWithDeviceAction.Internal.FingerprintPhraseReceived(
                result = UserFingerprintResult.Success(newFingerprint),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = LoginWithDeviceState.ViewState.Content(
                    fingerprintPhrase = newFingerprint,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on fingerprint result failure received should show error`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        viewModel.actionChannel.trySend(
            LoginWithDeviceAction.Internal.FingerprintPhraseReceived(
                result = UserFingerprintResult.Error,
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = LoginWithDeviceState.ViewState.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: LoginWithDeviceState = DEFAULT_STATE,
    ): LoginWithDeviceViewModel =
        LoginWithDeviceViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle().apply { set("state", state) },
        )

    companion object {
        private const val EMAIL = "test@gmail.com"
        private val DEFAULT_STATE = LoginWithDeviceState(
            emailAddress = EMAIL,
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = "initialFingerprint",
            ),
        )
    }
}
