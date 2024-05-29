package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
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

    @Test
    @Suppress("MaxLineLength")
    fun `on DeleteAccountClick with DeleteAccountResult Success should set dialog to Success`() =
        runTest {
            coEvery {
                authRepo.deleteAccountWithOneTimePassword("123456")
            } returns DeleteAccountResult.Success
            val initialState = DEFAULT_STATE.copy(
                verificationCode = "123456",
            )
            val viewModel = createViewModel(state = initialState)
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    DeleteAccountConfirmationAction.DeleteAccountClick,
                )
                assertEquals(
                    initialState.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess(),
                    ),
                    awaitItem(),
                )
            }
            coVerify { authRepo.deleteAccountWithOneTimePassword("123456") }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on DeleteAccountClick with DeleteAccountResult Error should set dialog to Error with message`() =
        runTest {
            coEvery {
                authRepo.deleteAccountWithOneTimePassword("123456")
            } returns DeleteAccountResult.Error(message = "Delete account error")
            val initialState = DEFAULT_STATE.copy(
                verificationCode = "123456",
            )
            val viewModel = createViewModel(
                state = initialState,
            )
            viewModel.stateFlow.test {
                assertEquals(initialState, awaitItem())
                viewModel.trySendAction(
                    DeleteAccountConfirmationAction.DeleteAccountClick,
                )
                assertEquals(
                    initialState.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    initialState.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                            message = "Delete account error".asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify { authRepo.deleteAccountWithOneTimePassword("123456") }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on ResendCodeClick with requestOneTimePasscode Success should set dialog to null`() =
        runTest {
            coEvery {
                authRepo.requestOneTimePasscode()
            } returns RequestOtpResult.Success
            val viewModel = createViewModel(state = DEFAULT_STATE)
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    DeleteAccountConfirmationAction.ResendCodeClick,
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
                    ),
                    awaitItem(),
                )
                assertEquals(DEFAULT_STATE, awaitItem())
            }
            coVerify { authRepo.requestOneTimePasscode() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on ResendCodeClick with requestOneTimePasscode Success should set dialog to Error`() =
        runTest {
            coEvery {
                authRepo.requestOneTimePasscode()
            } returns RequestOtpResult.Error(message = "Error")
            val viewModel = createViewModel(state = DEFAULT_STATE)
            viewModel.stateFlow.test {
                assertEquals(DEFAULT_STATE, awaitItem())
                viewModel.trySendAction(
                    DeleteAccountConfirmationAction.ResendCodeClick,
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
                    ),
                    awaitItem(),
                )
                assertEquals(
                    DEFAULT_STATE.copy(
                        dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    ),
                    awaitItem(),
                )
            }
            coVerify { authRepo.requestOneTimePasscode() }
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
    DeleteAccountConfirmationState(
        dialog = null,
        verificationCode = "",
    )
