package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequest
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class LoginWithDeviceViewModelTest : BaseViewModelTest() {

    private val authRepository = mockk<AuthRepository> {
        coEvery {
            createAuthRequest(EMAIL)
        } returns AuthRequestResult.Success(AUTH_REQUEST)
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        coVerify { authRepository.createAuthRequest(EMAIL) }
    }

    @Test
    fun `initial state should be correct when set`() = runTest {
        val newEmail = "newEmail@gmail.com"

        coEvery {
            authRepository.createAuthRequest(newEmail)
        } returns AuthRequestResult.Success(AUTH_REQUEST)
        val state = LoginWithDeviceState(
            emailAddress = newEmail,
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = FINGERPRINT,
                isResendNotificationLoading = false,
                shouldShowErrorDialog = false,
            ),
        )
        val viewModel = createViewModel(state)
        viewModel.stateFlow.test {
            assertEquals(state, awaitItem())
        }
        coVerify {
            authRepository.createAuthRequest(newEmail)
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
    fun `ResendNotificationClick should create new auth request and update state`() = runTest {
        val newFingerprint = "newFingerprint"
        coEvery {
            authRepository.createAuthRequest(EMAIL)
        } returns AuthRequestResult.Success(AUTH_REQUEST.copy(fingerprint = newFingerprint))
        val viewModel = createViewModel()
        viewModel.actionChannel.trySend(LoginWithDeviceAction.ResendNotificationClick)
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = LoginWithDeviceState.ViewState.Content(
                    fingerprintPhrase = newFingerprint,
                    isResendNotificationLoading = false,
                    shouldShowErrorDialog = false,
                ),
            ),
            viewModel.stateFlow.value,
        )
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
    fun `on auth request result success received should show content`() = runTest {
        val newFingerprint = "newFingerprint"
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        viewModel.actionChannel.trySend(
            LoginWithDeviceAction.Internal.NewAuthRequestResultReceive(
                result = AuthRequestResult.Success(
                    authRequest = mockk<AuthRequest> {
                        every { fingerprint } returns newFingerprint
                    },
                ),
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = LoginWithDeviceState.ViewState.Content(
                    fingerprintPhrase = newFingerprint,
                    isResendNotificationLoading = false,
                    shouldShowErrorDialog = false,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `on fingerprint result failure received should show error dialog`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        viewModel.actionChannel.trySend(
            LoginWithDeviceAction.Internal.NewAuthRequestResultReceive(
                result = AuthRequestResult.Error,
            ),
        )
        assertEquals(
            DEFAULT_STATE.copy(
                viewState = LoginWithDeviceState.ViewState.Content(
                    fingerprintPhrase = "",
                    isResendNotificationLoading = false,
                    shouldShowErrorDialog = true,
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
        private const val FINGERPRINT = "fingerprint"
        private val DEFAULT_STATE = LoginWithDeviceState(
            emailAddress = EMAIL,
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = FINGERPRINT,
                isResendNotificationLoading = false,
                shouldShowErrorDialog = false,
            ),
        )
        private val AUTH_REQUEST = AuthRequest(
            id = "1",
            publicKey = "2",
            platform = "Android",
            ipAddress = "192.168.0.1",
            key = "public",
            masterPasswordHash = "verySecureHash",
            creationDate = ZonedDateTime.parse("2024-09-13T00:00Z"),
            responseDate = null,
            requestApproved = true,
            originUrl = "www.bitwarden.com",
            fingerprint = FINGERPRINT,
        )
    }
}
