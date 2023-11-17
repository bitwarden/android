package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
    fun `on DeleteAccountClick should make the delete call`() = runTest {
        val viewModel = createViewModel()
        val masterPassword = "ckasb kcs ja"
        coEvery { authRepo.deleteAccount(masterPassword) } returns Unit.asSuccess()

        viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick(masterPassword))

        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        coVerify {
            authRepo.deleteAccount(masterPassword)
        }
    }

    @Test
    fun `on DeleteAccountClick should update dialog state`() = runTest {
        val viewModel = createViewModel()
        val masterPassword = "ckasb kcs ja"
        coEvery { authRepo.deleteAccount(masterPassword) } returns Throwable("Fail").asFailure()

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
