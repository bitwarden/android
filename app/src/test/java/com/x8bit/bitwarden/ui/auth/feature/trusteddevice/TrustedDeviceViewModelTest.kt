package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TrustedDeviceViewModelTest : BaseViewModelTest() {

    private val environmentRepo: FakeEnvironmentRepository = FakeEnvironmentRepository()

    @Test
    fun `on BackClick emits NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.BackClick)
            assertEquals(TrustedDeviceEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on RememberToggle updates the isRemembered state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
            viewModel.trySendAction(TrustedDeviceAction.RememberToggle(isRemembered = true))
            assertEquals(DEFAULT_STATE.copy(isRemembered = true), awaitItem())
            viewModel.trySendAction(TrustedDeviceAction.RememberToggle(isRemembered = false))
            assertEquals(DEFAULT_STATE.copy(isRemembered = false), awaitItem())
        }
    }

    @Test
    fun `on ApproveWithAdminClick emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithAdminClick)
            assertEquals(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `on ApproveWithDeviceClick emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithDeviceClick)
            assertEquals(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `on ApproveWithPasswordClick emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.ApproveWithPasswordClick)
            assertEquals(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    @Test
    fun `on NotYouClick emits ShowToast`() = runTest {
        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(TrustedDeviceAction.NotYouClick)
            assertEquals(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()), awaitItem())
        }
    }

    private fun createViewModel(
        state: TrustedDeviceState? = null,
        environmentRepository: EnvironmentRepository = environmentRepo,
    ): TrustedDeviceViewModel =
        TrustedDeviceViewModel(
            savedStateHandle = SavedStateHandle().apply {
                set("state", state)
                set("email_address", "email@bitwarden.com")
            },
            environmentRepository = environmentRepository,
        )
}

private val DEFAULT_STATE: TrustedDeviceState = TrustedDeviceState(
    emailAddress = "email@bitwarden.com",
    environmentLabel = "bitwarden.com",
    isRemembered = false,
)
