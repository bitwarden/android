package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.lifecycle.SavedStateHandle
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
    fun `initial state should be correct`() {
        val viewModel = AccountSecurityViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockk(),
        )
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = AccountSecurityViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockk(),
        )
        viewModel.eventFlow.test {
            viewModel.trySendAction(AccountSecurityAction.BackClick)
            assertEquals(AccountSecurityEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on LogoutClick should show confirm log out dialog`() = runTest {
        val viewModel = AccountSecurityViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockk(),
        )
        viewModel.trySendAction(AccountSecurityAction.LogoutClick)
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    shouldShowConfirmLogoutDialog = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `on ConfirmLogoutClick should call logout and hide confirm dialog`() = runTest {
        val authRepository: AuthRepository = mockk {
            every { logout() } returns Unit
        }
        val viewModel = AccountSecurityViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = authRepository,
        )
        viewModel.trySendAction(AccountSecurityAction.ConfirmLogoutClick)
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    shouldShowConfirmLogoutDialog = false,
                ),
                awaitItem(),
            )
        }
        verify { authRepository.logout() }
    }

    @Test
    fun `on DismissDialog should hide dialog`() = runTest {
        val viewModel = AccountSecurityViewModel(
            savedStateHandle = SavedStateHandle(),
            authRepository = mockk(),
        )
        viewModel.trySendAction(AccountSecurityAction.DismissDialog)
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    shouldShowConfirmLogoutDialog = false,
                ),
                awaitItem(),
            )
        }
    }

    companion object {
        private val DEFAULT_STATE = AccountSecurityState(
            shouldShowConfirmLogoutDialog = false,
        )
    }
}
