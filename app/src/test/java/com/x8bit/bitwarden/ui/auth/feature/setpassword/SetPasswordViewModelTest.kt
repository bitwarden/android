package com.x8bit.bitwarden.ui.auth.feature.setpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
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

class SetPasswordViewModelTest : BaseViewModelTest() {
    private val authRepository: AuthRepository = mockk {
        every { passwordPolicies } returns emptyList()
        every { ssoOrganizationIdentifier } returns ORGANIZATION_IDENTIFIER
    }

    @Test
    fun `null ssoOrganizationIdentifier logs user out`() = runTest {
        every { authRepository.logout() } just runs
        every { authRepository.ssoOrganizationIdentifier } returns null
        createViewModel()
        verify {
            authRepository.logout()
            authRepository.ssoOrganizationIdentifier
        }
    }

    @Test
    fun `CancelClick calls logout`() = runTest {
        every { authRepository.logout() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.CancelClick)
        verify { authRepository.logout() }
    }

    @Test
    fun `SubmitClicked with blank password shows error alert`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.validation_field_required
                        .asText(R.string.master_password.asText()),
                ),
            ),
            viewModel.stateFlow.value,
        )

        // Dismiss the alert.
        viewModel.trySendAction(SetPasswordAction.DialogDismiss)
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid password shows error alert for short password`() = runTest {
        val password = "TestPass"

        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged(password))
        viewModel.trySendAction(SetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_length_val_message_x
                        .asText(MIN_PASSWORD_LENGTH),
                ),
                passwordInput = password,
                retypePasswordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with non-matching retyped password shows error alert`() = runTest {
        val password = "TestPassword123"
        coEvery {
            authRepository.validatePasswordAgainstPolicies(password)
        } returns true

        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.PasswordInputChanged(password))

        viewModel.trySendAction(SetPasswordAction.SubmitClick)

        assertEquals(
            DEFAULT_STATE.copy(
                dialogState = SetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_confirmation_val_message.asText(),
                ),
                passwordInput = password,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `SubmitClicked with invalid password shows error alert for weak password reason`() =
        runTest {
            val password = "Test123"
            coEvery {
                authRepository.validatePasswordAgainstPolicies(password)
            } returns false

            val viewModel = createViewModel(
                state = SetPasswordState(
                    organizationIdentifier = ORGANIZATION_IDENTIFIER,
                    policies = listOf(
                        R.string.policy_in_effect_uppercase.asText(),
                    ),
                    dialogState = null,
                    passwordInput = "",
                    retypePasswordInput = "",
                    passwordHintInput = "",
                ),
            )

            viewModel.trySendAction(SetPasswordAction.PasswordInputChanged(password))
            viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged(password))
            viewModel.trySendAction(SetPasswordAction.SubmitClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = SetPasswordState.DialogState.Error(
                        title = R.string.master_password_policy_validation_title.asText(),
                        message = R.string.master_password_policy_validation_message.asText(),
                    ),
                    passwordInput = password,
                    retypePasswordInput = password,
                    policies = listOf(
                        R.string.policy_in_effect_uppercase.asText(),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            coVerify {
                authRepository.validatePasswordAgainstPolicies(password)
            }
        }

    @Test
    fun `SubmitClicked with all valid inputs sets password`() = runTest {
        val password = "TestPassword123"
        coEvery {
            authRepository.setPassword(
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
                password = password,
                passwordHint = "",
            )
        } returns SetPasswordResult.Success

        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.PasswordInputChanged(password))
        viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged(password))

        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = null,
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )

            viewModel.trySendAction(SetPasswordAction.SubmitClick)

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = SetPasswordState.DialogState.Loading(
                        message = R.string.updating_password.asText(),
                    ),
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )

            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = null,
                    passwordInput = password,
                    retypePasswordInput = password,
                ),
                awaitItem(),
            )
        }

        coVerify {
            authRepository.setPassword(
                organizationIdentifier = ORGANIZATION_IDENTIFIER,
                password = password,
                passwordHint = "",
            )
        }
    }

    @Test
    fun `PasswordInputChanged should update the password input in the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.PasswordInputChanged("TestPassword123"))

        assertEquals(
            DEFAULT_STATE.copy(
                passwordInput = "TestPassword123",
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `RetypePasswordInputChanged should update the retype password input in the state`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.trySendAction(SetPasswordAction.RetypePasswordInputChanged("TestPassword123"))

            assertEquals(
                DEFAULT_STATE.copy(
                    retypePasswordInput = "TestPassword123",
                ),
                viewModel.stateFlow.value,
            )
        }

    @Test
    fun `PasswordHintInputChanged should update the password hint input in the state`() = runTest {
        val viewModel = createViewModel()
        viewModel.trySendAction(SetPasswordAction.PasswordHintInputChanged("TestPassword123"))

        assertEquals(
            DEFAULT_STATE.copy(
                passwordHintInput = "TestPassword123",
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        state: SetPasswordState? = null,
    ): SetPasswordViewModel =
        SetPasswordViewModel(
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle(mapOf("state" to state)),
        )
}

private const val MIN_PASSWORD_LENGTH = 12
private const val ORGANIZATION_IDENTIFIER: String = "orgId"
private val DEFAULT_STATE = SetPasswordState(
    organizationIdentifier = ORGANIZATION_IDENTIFIER,
    policies = emptyList(),
    dialogState = null,
    passwordInput = "",
    retypePasswordInput = "",
    passwordHintInput = "",
)
