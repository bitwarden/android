package com.x8bit.bitwarden.ui.auth.feature.resetPassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordAction
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordEvent
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordState
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordViewModel
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.util.toDisplayLabels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResetPasswordViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk {
        every { passwordPolicies } returns emptyList()
        every { passwordResetReason } returns ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN
    }

    private val vaultRepository: VaultRepository = mockk {
        coEvery { syncForResult() } returns SyncVaultDataResult.Success(itemsAvailable = true)
    }

    @Test
    fun `initial state should be Loading and transition to Content on sync success`() = runTest {
        val syncResult = CompletableDeferred<SyncVaultDataResult>()
        coEvery { vaultRepository.syncForResult() } coAnswers { syncResult.await() }

        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(viewState = ResetPasswordState.ViewState.Loading),
                awaitItem(),
            )
            syncResult.complete(SyncVaultDataResult.Success(itemsAvailable = true))
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        coVerify(exactly = 1) { vaultRepository.syncForResult() }
    }

    @Test
    fun `sync success should populate the content from the latest policies and reset reason`() {
        val policy = PolicyInformation.MasterPassword(
            minLength = 20,
            minComplexity = null,
            requireUpper = null,
            requireLower = null,
            requireNumbers = null,
            requireSpecial = null,
            enforceOnLogin = true,
        )
        every { authRepository.passwordPolicies } returns listOf(policy)
        every {
            authRepository.passwordResetReason
        } returns ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
                    policies = listOf(policy).toDisplayLabels(),
                    minimumPasswordLength = 20,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `sync error should display the error view state`() {
        coEvery {
            vaultRepository.syncForResult()
        } returns SyncVaultDataResult.Error(throwable = Throwable("Fail!"))

        val viewModel = createViewModel()

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = ResetPasswordState.ViewState.Error(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `RetryClick should display Loading and sync again`() = runTest {
        coEvery {
            vaultRepository.syncForResult()
        } returns SyncVaultDataResult.Error(throwable = Throwable("Fail!"))
        val viewModel = createViewModel()
        val syncResult = CompletableDeferred<SyncVaultDataResult>()
        coEvery { vaultRepository.syncForResult() } coAnswers { syncResult.await() }

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = ResetPasswordState.ViewState.Error(
                        message = BitwardenString.generic_error_message.asText(),
                    ),
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ResetPasswordAction.RetryClick)
            assertEquals(
                DEFAULT_STATE.copy(viewState = ResetPasswordState.ViewState.Loading),
                awaitItem(),
            )

            syncResult.complete(SyncVaultDataResult.Success(itemsAvailable = true))
            assertEquals(DEFAULT_STATE, awaitItem())
        }
        coVerify(exactly = 2) { vaultRepository.syncForResult() }
    }

    @Test
    fun `input and save actions should be ignored when not in the Content state`() {
        coEvery {
            vaultRepository.syncForResult()
        } returns SyncVaultDataResult.Error(throwable = Throwable("Fail!"))
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_4)
        val viewModel = createViewModel()
        val errorState = DEFAULT_STATE.copy(
            viewState = ResetPasswordState.ViewState.Error(
                message = BitwardenString.generic_error_message.asText(),
            ),
        )

        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged("Test123"))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged("Test123"))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged("Test123"))
        viewModel.trySendAction(ResetPasswordAction.PasswordHintInputChanged("Test123"))
        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(errorState, viewModel.stateFlow.value)
    }

    @Test
    fun `ConfirmLogoutClick logs out`() = runTest {
        every { authRepository.logout(reason = any()) } just runs

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.ConfirmLogoutClick)

        verify(exactly = 1) {
            authRepository.logout(
                reason = LogoutReason.Click(source = "ResetPasswordViewModel"),
            )
        }
    }

    @Test
    fun `CurrentPasswordInputChanged should update the current password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    currentPasswordInput = "Test123",
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with blank password shows error alert`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.validation_field_required
                        .asText(BitwardenString.master_password.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )

        // Dismiss the alert.
        viewModel.trySendAction(ResetPasswordAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid password shows error alert for weak password reason`() {
        val password = "Test123"
        every {
            authRepository.validatePasswordAgainstPolicies(password = password)
        } returns false
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    passwordInput = password,
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.master_password_policy_validation_title.asText(),
                    message = BitwardenString.master_password_policy_validation_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid password shows error alert for admin reset reason`() {
        val password = "Test123"
        every {
            authRepository.passwordResetReason
        } returns ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
                    passwordInput = password,
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenPlurals.master_password_length_val_message_x.asPluralsText(
                        quantity = MIN_PASSWORD_LENGTH,
                        args = arrayOf(MIN_PASSWORD_LENGTH),
                    ),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with non-matching retyped password shows error alert`() {
        val password = "Test123"
        every {
            authRepository.validatePasswordAgainstPolicies(password = password)
        } returns true
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    passwordInput = password,
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.master_password_confirmation_val_message.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with error for validating current password shows error alert`() {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        val error = Throwable("Fail!")
        every {
            authRepository.validatePasswordAgainstPolicies(password = password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Error(error = error)
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    currentPasswordInput = currentPassword,
                    passwordInput = password,
                    retypePasswordInput = password,
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                    error = error,
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid current password shows alert`() {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        every {
            authRepository.validatePasswordAgainstPolicies(password = password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Success(isValid = false)
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SaveClick)

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    currentPasswordInput = currentPassword,
                    passwordInput = password,
                    retypePasswordInput = password,
                    passwordStrengthState = PasswordStrengthState.WEAK_1,
                ),
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.invalid_master_password.asText(),
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with all valid inputs resets password`() = runTest {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        every {
            authRepository.validatePasswordAgainstPolicies(password = password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Success(isValid = true)
        coEvery {
            authRepository.resetPassword(any(), any(), any())
        } returns ResetPasswordResult.Success
        every {
            authRepository.getPasswordStrength(password = any())
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_0)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT.copy(
                        currentPasswordInput = currentPassword,
                        passwordInput = password,
                        retypePasswordInput = password,
                        passwordStrengthState = PasswordStrengthState.WEAK_1,
                    ),
                    dialogState = null,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ResetPasswordAction.SaveClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT.copy(
                        currentPasswordInput = currentPassword,
                        passwordInput = password,
                        retypePasswordInput = password,
                        passwordStrengthState = PasswordStrengthState.WEAK_1,
                    ),
                    dialogState = ResetPasswordState.DialogState.Loading(
                        message = BitwardenString.updating_password.asText(),
                    ),
                ),
                awaitItem(),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    viewState = DEFAULT_CONTENT.copy(
                        currentPasswordInput = currentPassword,
                        passwordInput = password,
                        retypePasswordInput = password,
                        passwordStrengthState = PasswordStrengthState.WEAK_1,
                    ),
                    dialogState = null,
                ),
                awaitItem(),
            )

            coVerify { authRepository.resetPassword(any(), any(), any()) }
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `PasswordInputChanged should update the password input in the state along with the password strength state`() {
        val passwordInput = "Test123"
        every {
            authRepository.getPasswordStrength(password = passwordInput)
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_4)
        val viewModel = createViewModel()

        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(input = passwordInput))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    passwordInput = passwordInput,
                    passwordStrengthState = PasswordStrengthState.STRONG,
                ),
            ),
            viewModel.stateFlow.value,
        )
        verify(exactly = 1) { authRepository.getPasswordStrength(password = passwordInput) }
    }

    @Test
    fun `PasswordInputChanged with empty input should reset the password strength state`() {
        every {
            authRepository.getPasswordStrength(password = "a")
        } returns PasswordStrengthResult.Success(passwordStrength = PasswordStrength.LEVEL_4)
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(input = "a"))

        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(input = ""))

        assertEquals(DEFAULT_STATE, viewModel.stateFlow.value)
        verify(exactly = 0) { authRepository.getPasswordStrength(password = "") }
    }

    @Test
    fun `PasswordInputChanged with password strength error should not update the strength`() {
        every {
            authRepository.getPasswordStrength(password = "Test123")
        } returns PasswordStrengthResult.Error(error = Throwable("Fail!"))
        val viewModel = createViewModel()

        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(input = "Test123"))

        assertEquals(
            DEFAULT_STATE.copy(viewState = DEFAULT_CONTENT.copy(passwordInput = "Test123")),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `RetypePasswordInputChanged should update the retype password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    retypePasswordInput = "Test123",
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `PasswordHintInputChanged should update the password hint input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordHintInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                viewState = DEFAULT_CONTENT.copy(
                    passwordHintInput = "Test123",
                ),
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `LearnHowPreventLockoutClick action sends NavigateToPreventAccountLockout event`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(ResetPasswordAction.LearnHowPreventLockoutClick)
                assertEquals(
                    ResetPasswordEvent.NavigateToPreventAccountLockout,
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(
        initialState: ResetPasswordState? = null,
    ): ResetPasswordViewModel =
        ResetPasswordViewModel(
            authRepository = authRepository,
            vaultRepository = vaultRepository,
            savedStateHandle = SavedStateHandle().apply { set("state", initialState) },
        )
}

private const val MIN_PASSWORD_LENGTH = 12
private val DEFAULT_CONTENT = ResetPasswordState.ViewState.Content(
    resetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
    policies = persistentListOf(),
    minimumPasswordLength = MIN_PASSWORD_LENGTH,
    currentPasswordInput = "",
    passwordInput = "",
    retypePasswordInput = "",
    passwordHintInput = "",
    passwordStrengthState = PasswordStrengthState.NONE,
)
private val DEFAULT_STATE = ResetPasswordState(
    viewState = DEFAULT_CONTENT,
    dialogState = null,
)
