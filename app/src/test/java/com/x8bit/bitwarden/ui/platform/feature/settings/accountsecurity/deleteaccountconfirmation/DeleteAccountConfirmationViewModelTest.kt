package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteAccountConfirmationViewModelTest : BaseViewModelTest() {

    private val authRepo: AuthRepository = mockk(relaxed = true)

    @Test
    fun `initial state should be correct when not set`() {
        val viewModel = createViewModel(state = null)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `initial state should be correct when set`() {
        val state = DEFAULT_STATE.copy(
            dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                message = "Hello".asText(),
            ),
        )
        val viewModel = createViewModel(state = state)
        assertEquals(state, viewModel.stateFlow.value)
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(DeleteAccountConfirmationAction.CloseClick)
            assertEquals(DeleteAccountConfirmationEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `DeleteAccountAcknowledge should clear dialog and call clearPendingAccountDeletion`() =
        runTest {
            every { authRepo.clearPendingAccountDeletion() } just runs
            val state = DEFAULT_STATE.copy(
                dialog =
                DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess(),
            )
            val viewModel = createViewModel(state = state)

            viewModel.trySendAction(DeleteAccountConfirmationAction.DeleteAccountAcknowledge)
            assertEquals(
                DEFAULT_STATE,
                viewModel.stateFlow.value,
            )
            verify {
                authRepo.clearPendingAccountDeletion()
            }
        }

    @Test
    fun `on DismissDialog should clear dialog state`() = runTest {
        val state = DEFAULT_STATE.copy(
            dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                message = "Hello".asText(),
            ),
        )
        val viewModel = createViewModel(state = state)

        viewModel.trySendAction(DeleteAccountConfirmationAction.DismissDialog)
        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
    }

    private fun createViewModel(
        authenticationRepository: AuthRepository = authRepo,
        state: DeleteAccountConfirmationState? = null,
    ): DeleteAccountConfirmationViewModel = DeleteAccountConfirmationViewModel(
        authRepository = authenticationRepository,
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: DeleteAccountConfirmationState =
    DeleteAccountConfirmationState(dialog = null)
