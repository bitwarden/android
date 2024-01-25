package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteAccountViewModelTest : BaseViewModelTest() {

    private val authRepo: AuthRepository = mockk(relaxed = true)

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            dialog = DeleteAccountState.DeleteAccountDialog.Error("Hello".asText()),
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on CancelClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountAction.CancelClick)
            assertEquals(DeleteAccountEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountAction.CloseClick)
            assertEquals(DeleteAccountEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on DeleteAccountClick should update dialog state when delete account succeeds`() =
        runTest {
            val viewModel = createViewModel()
            val masterPassword = "ckasb kcs ja"
            coEvery { authRepo.deleteAccount(masterPassword) } returns DeleteAccountResult.Success

            viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick(masterPassword))

            assertEquals(
                DEFAULT_STATE.copy(dialog = DeleteAccountState.DeleteAccountDialog.DeleteSuccess),
                viewModel.stateFlow.value,
            )

            coVerify {
                authRepo.deleteAccount(masterPassword)
            }
        }

    @Test
    fun `on DeleteAccountClick should update dialog state when deleteAccount fails`() = runTest {
        val viewModel = createViewModel()
        val masterPassword = "ckasb kcs ja"
        coEvery { authRepo.deleteAccount(masterPassword) } returns DeleteAccountResult.Error

        viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick(masterPassword))

        assertEquals(
            DEFAULT_STATE.copy(
                dialog = DeleteAccountState.DeleteAccountDialog.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        coVerify {
            authRepo.deleteAccount(masterPassword)
        }
    }

    @Test
    fun `AccountDeletionConfirm should clear dialog state and call clearPendingAccountDeletion`() =
        runTest {
            every { authRepo.clearPendingAccountDeletion() } just runs
            val state = DEFAULT_STATE.copy(
                dialog = DeleteAccountState.DeleteAccountDialog.DeleteSuccess,
            )
            val viewModel = createViewModel(state = state)

            viewModel.trySendAction(DeleteAccountAction.AccountDeletionConfirm)
            assertEquals(
                DEFAULT_STATE.copy(dialog = null),
                viewModel.stateFlow.value,
            )
            verify {
                authRepo.clearPendingAccountDeletion()
            }
        }

    @Test
    fun `on DismissDialog should clear dialog state`() = runTest {
        val state = DEFAULT_STATE.copy(
            dialog = DeleteAccountState.DeleteAccountDialog.Error("Hello".asText()),
        )
        val viewModel = createViewModel(state = state)

        viewModel.trySendAction(DeleteAccountAction.DismissDialog)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    private fun createViewModel(
        authenticationRepository: AuthRepository = authRepo,
        state: DeleteAccountState? = DEFAULT_STATE,
    ): DeleteAccountViewModel = DeleteAccountViewModel(
        authRepository = authenticationRepository,
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: DeleteAccountState = DeleteAccountState(
    dialog = null,
)
