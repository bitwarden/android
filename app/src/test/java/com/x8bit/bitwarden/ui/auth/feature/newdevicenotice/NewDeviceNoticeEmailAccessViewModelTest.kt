package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewDeviceNoticeEmailAccessViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should be correct with email from state handle`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `EmailAccessToggle should update value of isEmailAccessEnabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.EmailAccessToggle(true))
            assertEquals(
                viewModel.stateFlow.value,
                DEFAULT_STATE.copy(isEmailAccessEnabled = true),
            )
        }
    }

    @Test
    fun `ContinueClick with valid email should emit NavigateToTwoFactorOptions`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeEmailAccessAction.ContinueClick)
            assertEquals(
                NewDeviceNoticeEmailAccessEvent.NavigateToTwoFactorOptions,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle().also {
            it["email_address"] = EMAIL
        },
    ): NewDeviceNoticeEmailAccessViewModel = NewDeviceNoticeEmailAccessViewModel(
        savedStateHandle = savedStateHandle,
    )
}

private const val EMAIL = "active@bitwarden.com"

private val DEFAULT_STATE =
    NewDeviceNoticeEmailAccessState(
        email = EMAIL,
        isEmailAccessEnabled = false,
    )
