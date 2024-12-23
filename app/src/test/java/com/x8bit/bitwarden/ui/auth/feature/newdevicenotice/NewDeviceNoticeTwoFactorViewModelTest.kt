package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewDeviceNoticeTwoFactorViewModelTest : BaseViewModelTest() {
    private val environmentRepository = FakeEnvironmentRepository()
    private val authRepository = mockk<AuthRepository> {
        every { getNewDeviceNoticeState() } returns NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN,
            delayDate = null,
        )
        every { setNewDeviceNoticeState(any()) } just runs
    }

    @Test
    fun `ChangeAccountEmailClick should emit NavigateToChangeAccountEmail`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.ChangeAccountEmailClick)
            assertEquals(
                NewDeviceNoticeTwoFactorEvent.NavigateToChangeAccountEmail(
                    url = "https://vault.bitwarden.com/#/settings/account",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `TurnOnTwoFactorClick should emit NavigateToTurnOnTwoFactor`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.TurnOnTwoFactorClick)
            assertEquals(
                NewDeviceNoticeTwoFactorEvent.NavigateToTurnOnTwoFactor(
                    url = "https://vault.bitwarden.com/#/settings/security/two-factor",
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `RemindMeLaterClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(NewDeviceNoticeTwoFactorAction.RemindMeLaterClick)
            assertEquals(
                NewDeviceNoticeTwoFactorEvent.NavigateBackToVault,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): NewDeviceNoticeTwoFactorViewModel =
        NewDeviceNoticeTwoFactorViewModel(
            authRepository = authRepository,
            environmentRepository = environmentRepository,
        )
}
