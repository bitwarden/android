package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginWithDeviceViewModelTest : BaseViewModelTest() {

    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
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

    private fun createViewModel(): LoginWithDeviceViewModel =
        LoginWithDeviceViewModel(
            savedStateHandle = savedStateHandle,
        )

    companion object {
        private val DEFAULT_STATE = LoginWithDeviceState(
            viewState = LoginWithDeviceState.ViewState.Content(
                fingerprintPhrase = "alabster-drinkable-mystified-rapping-irrigate",
            ),
        )
    }
}
