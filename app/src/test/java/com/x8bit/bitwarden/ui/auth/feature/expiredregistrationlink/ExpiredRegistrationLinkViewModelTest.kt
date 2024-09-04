package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpiredRegistrationLinkViewModelTest : BaseViewModelTest() {

    private val authRepository = mockk<AuthRepository>(relaxed = true)

    @Test
    fun `CloseClicked sends NavigateBack event and resets pending account addition`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ExpiredRegistrationLinkAction.CloseClicked)
            assertEquals(ExpiredRegistrationLinkEvent.NavigateBack, awaitItem())
        }
        verify { authRepository.hasPendingAccountAddition = true }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `RestartRegistrationClicked sends NavigateToStartRegistration event and resets pending account addition`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(ExpiredRegistrationLinkAction.RestartRegistrationClicked)
                assertEquals(ExpiredRegistrationLinkEvent.NavigateToStartRegistration, awaitItem())
            }
            verify { authRepository.hasPendingAccountAddition = true }
        }

    @Test
    fun `GoToLoginClicked sends NavigateToLogin event and resets pending account addition`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(ExpiredRegistrationLinkAction.GoToLoginClicked)
                assertEquals(ExpiredRegistrationLinkEvent.NavigateToLogin, awaitItem())
            }
            verify { authRepository.hasPendingAccountAddition = true }
        }

    private fun createViewModel(): ExpiredRegistrationLinkViewModel =
        ExpiredRegistrationLinkViewModel(authRepository = authRepository)
}
