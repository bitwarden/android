package com.x8bit.bitwarden.ui.platform.feature.settings

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountSecurityViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = AccountSecurityViewModel(
            authRepository = mockk(),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.BackClick)
            assertEquals(AccountSecurityEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on LogoutClick should call logout`() = runTest {
        val authRepository: AuthRepository = mockk {
            every { logout() } returns Unit
        }
        val viewModel = AccountSecurityViewModel(
            authRepository = authRepository,
        )
        viewModel.trySendAction(AccountSecurityAction.LogoutClick)
        verify { authRepository.logout() }
    }
}
