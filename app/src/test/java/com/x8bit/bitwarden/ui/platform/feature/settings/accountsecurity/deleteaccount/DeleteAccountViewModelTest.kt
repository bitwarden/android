package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeleteAccountViewModelTest : BaseViewModelTest() {
    private val mutableUserStateFlow = MutableStateFlow(DEFAULT_USER_STATE)
    private val authRepo: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    @Test
    fun `initial state should be correct when not set`() {
        mutableUserStateFlow.update { currentState ->
            currentState.copy(
                accounts = currentState.accounts.map { account ->
                    account.copy(hasMasterPassword = false)
                },
            )
        }
        val viewModel = createViewModel(state = null)
        assertEquals(
            DEFAULT_STATE.copy(isUnlockWithPasswordEnabled = false),
            viewModel.stateFlow.value,
        )
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
    @Suppress("MaxLineLength")
    fun `on DeleteAccountConfirmDialogClick should update dialog state when delete account succeeds`() =
        runTest {
            val viewModel = createViewModel()
            val masterPassword = "ckasb kcs ja"
            coEvery {
                authRepo.validatePassword(any())
            } returns ValidatePasswordResult.Success(isValid = true)
            coEvery {
                authRepo.deleteAccountWithMasterPassword(masterPassword)
            } returns DeleteAccountResult.Success

            viewModel.trySendAction(
                DeleteAccountAction.DeleteAccountConfirmDialogClick(
                    masterPassword,
                ),
            )

            assertEquals(
                DEFAULT_STATE.copy(dialog = DeleteAccountState.DeleteAccountDialog.DeleteSuccess),
                viewModel.stateFlow.value,
            )

            coVerify {
                authRepo.deleteAccountWithMasterPassword(masterPassword)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `on DeleteAccountClick should emit NavigateToDeleteAccountConfirmationScreen`() =
        runTest {
            val viewModel = createViewModel(
                state = DEFAULT_STATE.copy(
                    isUnlockWithPasswordEnabled = false,
                ),
            )

            viewModel.eventFlow.test {
                viewModel.trySendAction(DeleteAccountAction.DeleteAccountClick)
                assertEquals(
                    DeleteAccountEvent.NavigateToDeleteAccountConfirmationScreen,
                    awaitItem(),
                )
            }
        }

    @Test
    fun `on DeleteAccountClick should update dialog state when deleteAccount fails`() = runTest {
        val viewModel = createViewModel()
        val masterPassword = "ckasb kcs ja"
        coEvery {
            authRepo.validatePassword(any())
        } returns ValidatePasswordResult.Success(isValid = true)
        coEvery {
            authRepo.deleteAccountWithMasterPassword(masterPassword)
        } returns DeleteAccountResult.Error(message = null)

        viewModel.trySendAction(DeleteAccountAction.DeleteAccountConfirmDialogClick(masterPassword))

        assertEquals(
            DEFAULT_STATE.copy(
                dialog = DeleteAccountState.DeleteAccountDialog.Error(
                    message = R.string.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )

        coVerify {
            authRepo.deleteAccountWithMasterPassword(masterPassword)
        }
    }

    @Test
    fun `on DeleteAccountClick should update dialog state when invalid master pass is invalid`() =
        runTest {
            val viewModel = createViewModel()
            val masterPassword = "ckasb kcs ja"
            coEvery {
                authRepo.validatePassword(any())
            } returns ValidatePasswordResult.Success(isValid = false)
            coEvery {
                authRepo.deleteAccountWithMasterPassword(masterPassword)
            } returns DeleteAccountResult.Error(message = null)

            viewModel.trySendAction(
                DeleteAccountAction.DeleteAccountConfirmDialogClick(
                    masterPassword,
                ),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialog = DeleteAccountState.DeleteAccountDialog.Error(
                        message = R.string.invalid_master_password.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )

            coVerify(exactly = 0) {
                authRepo.deleteAccountWithMasterPassword(masterPassword)
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

private val DEFAULT_USER_STATE: UserState = UserState(
    activeUserId = "activeUserId",
    accounts = listOf(
        UserState.Account(
            userId = "activeUserId",
            name = "name",
            email = "email",
            avatarColorHex = "avatarColorHex",
            environment = Environment.Us,
            isPremium = true,
            isLoggedIn = true,
            isVaultUnlocked = true,
            needsPasswordReset = false,
            isBiometricsEnabled = false,
            organizations = emptyList(),
            needsMasterPassword = false,
            trustedDevice = UserState.TrustedDevice(
                isDeviceTrusted = true,
                hasAdminApproval = true,
                hasLoginApprovingDevice = true,
                hasResetPasswordPermission = true,
            ),
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        ),
    ),
)

private val DEFAULT_STATE: DeleteAccountState = DeleteAccountState(
    dialog = null,
    isUnlockWithPasswordEnabled = true,
)
