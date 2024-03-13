package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TrustedDeviceViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick emits NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.BackClick)
            assertEquals(TrustedDeviceEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: TrustedDeviceState? = null,
    ): TrustedDeviceViewModel =
        TrustedDeviceViewModel(
            savedStateHandle = SavedStateHandle().apply { set("state", state) },
        )
}
