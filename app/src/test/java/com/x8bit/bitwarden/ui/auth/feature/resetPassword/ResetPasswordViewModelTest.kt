package com.x8bit.bitwarden.ui.auth.feature.resetPassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordAction
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordState
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordViewModel
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

class ResetPasswordViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk {
        every { passwordPolicies } returns emptyList()
        every { passwordResetReason } returns ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN
    }

    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `ConfirmLogoutClick logs out`() = runTest {
        every { authRepository.logout() } just runs

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.ConfirmLogoutClick)

        verify { authRepository.logout() }
    }

    @Test
    fun `CurrentPasswordInputChanged should update the current password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                currentPasswordInput = "Test123",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with blank password shows error alert`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required
                        .asText(R.string.master_password.asText()),
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
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns false

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.master_password_policy_validation_title.asText(),
                    message = R.string.master_password_policy_validation_message.asText(),
                ),
                passwordInput = password,
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

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                resetReason = ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_length_val_message_x
                        .asText(MIN_PASSWORD_LENGTH),
                ),
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with non-matching retyped password shows error alert`() {
        val password = "Test123"
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns true

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_confirmation_val_message.asText(),
                ),
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with error for validating current password shows error alert`() {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Error

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.generic_error_message.asText(),
                ),
                currentPasswordInput = currentPassword,
                passwordInput = password,
                retypePasswordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid current password shows alert`() {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Success(isValid = false)

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.trySendAction(ResetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.invalid_master_password.asText(),
                ),
                currentPasswordInput = currentPassword,
                passwordInput = password,
                retypePasswordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with all valid inputs resets password`() = runTest {
        val currentPassword = "CurrentTest123"
        val password = "Test123"
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns true
        coEvery {
            authRepository.validatePassword(currentPassword)
        } returns ValidatePasswordResult.Success(isValid = true)
        coEvery {
            authRepository.resetPassword(any(), any(), any())
        } returns ResetPasswordResult.Success

        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.CurrentPasswordInputChanged(currentPassword))
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = null,
                    currentPasswordInput = currentPassword,
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(ResetPasswordAction.SubmitClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = ResetPasswordState.DialogState.Loading(
                        message = R.string.updating_password.asText(),
                    ),
                    currentPasswordInput = currentPassword,
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = null,
                    currentPasswordInput = currentPassword,
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )

            coVerify { authRepository.resetPassword(any(), any(), any()) }
        }
    }

    @Test
    fun `PasswordInputChanged should update the password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.PasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                passwordInput = "Test123",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `RetypePasswordInputChanged should update the retype password input in the state`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ResetPasswordAction.RetypePasswordInputChanged("Test123"))

        assertEquals(
            DEFAULT_STATE.copy(
                retypePasswordInput = "Test123",
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
                passwordHintInput = "Test123",
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(): ResetPasswordViewModel =
        ResetPasswordViewModel(
            authRepository = authRepository,
            savedStateHandle = savedStateHandle,
        )
}

private const val MIN_PASSWORD_LENGTH = 12
private val DEFAULT_STATE = ResetPasswordState(
    policies = emptyList(),
    resetReason = ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN,
    dialogState = null,
    currentPasswordInput = "",
    passwordInput = "",
    retypePasswordInput = "",
    passwordHintInput = "",
)
