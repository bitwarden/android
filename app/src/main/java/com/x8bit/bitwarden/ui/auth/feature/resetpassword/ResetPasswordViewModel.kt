package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.util.toDisplayLabels
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val MIN_PASSWORD_LENGTH = 12

/**
 * Manages application state for the Reset Password screen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ResetPasswordState, ResetPasswordEvent, ResetPasswordAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ResetPasswordState(
            policies = authRepository.passwordPolicies.toDisplayLabels(),
            resetReason = authRepository.passwordResetReason,
            dialogState = null,
            currentPasswordInput = "",
            passwordInput = "",
            retypePasswordInput = "",
            passwordHintInput = "",
        ),
) {
    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ResetPasswordAction) {
        when (action) {
            ResetPasswordAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
            ResetPasswordAction.SubmitClick -> handleSubmitClicked()
            ResetPasswordAction.DialogDismiss -> handleDialogDismiss()

            is ResetPasswordAction.CurrentPasswordInputChanged -> {
                handleCurrentPasswordInputChanged(action)
            }

            is ResetPasswordAction.PasswordInputChanged -> handlePasswordInputChanged(action)

            is ResetPasswordAction.RetypePasswordInputChanged -> {
                handleRetypePasswordInputChanged(action)
            }

            is ResetPasswordAction.PasswordHintInputChanged -> {
                handlePasswordHintInputChanged(action)
            }

            is ResetPasswordAction.Internal.ReceiveResetPasswordResult -> {
                handleReceiveResetPasswordResult(action)
            }

            is ResetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult -> {
                handleReceiveValidatePasswordAgainstPoliciesResult(action)
            }

            is ResetPasswordAction.Internal.ReceiveValidatePasswordResult -> {
                handleReceiveValidatePasswordResult(action)
            }
        }
    }

    /**
     * Dismiss the view if the user confirms logging out.
     */
    private fun handleConfirmLogoutClick() {
        authRepository.logout()
    }

    /**
     * Validate the user's current password when they submit.
     */
    private fun handleSubmitClicked() {
        // Display an error dialog if the new password field is blank.
        if (state.passwordInput.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = ResetPasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required
                            .asText(R.string.master_password.asText()),
                    ),
                )
            }
            return
        }

        // Check if the new password meets the policy requirements, if applicable.
        if (state.resetReason == ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN) {
            viewModelScope.launch {
                val result = authRepository.validatePasswordAgainstPolicies(state.passwordInput)
                sendAction(
                    ResetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult(
                        result,
                    ),
                )
            }
        } else {
            // Otherwise, simply verify that the password meets the minimum length requirement.
            if (state.passwordInput.length < MIN_PASSWORD_LENGTH) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.master_password_length_val_message_x
                                .asText(MIN_PASSWORD_LENGTH),
                        ),
                    )
                }
            } else {
                // Check that the re-typed password matches.
                if (!checkRetypedPassword()) return

                // Otherwise, if the password checks out, attempt to reset it.
                resetPassword()
            }
        }
    }

    /**
     * Dismiss the dialog state.
     */
    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
        }
    }

    /**
     * Update the state with the current password input.
     */
    private fun handleCurrentPasswordInputChanged(
        action: ResetPasswordAction.CurrentPasswordInputChanged,
    ) {
        mutableStateFlow.update {
            it.copy(
                currentPasswordInput = action.input,
            )
        }
    }

    /**
     * Update the state with the new password input.
     */
    private fun handlePasswordInputChanged(action: ResetPasswordAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(
                passwordInput = action.input,
            )
        }
    }

    /**
     * Update the state with the re-typed password input.
     */
    private fun handleRetypePasswordInputChanged(
        action: ResetPasswordAction.RetypePasswordInputChanged,
    ) {
        mutableStateFlow.update {
            it.copy(
                retypePasswordInput = action.input,
            )
        }
    }

    /**
     * Update the state with the password hint input.
     */
    private fun handlePasswordHintInputChanged(
        action: ResetPasswordAction.PasswordHintInputChanged,
    ) {
        mutableStateFlow.update {
            it.copy(
                passwordHintInput = action.input,
            )
        }
    }

    /**
     * Show an alert if the reset password attempt failed.
     */
    private fun handleReceiveResetPasswordResult(
        action: ResetPasswordAction.Internal.ReceiveResetPasswordResult,
    ) {
        // End the loading state.
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (action.result) {
            // Display an alert if there was an error.
            ResetPasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            // NO-OP: The root nav view model will handle the completed auth flow.
            ResetPasswordResult.Success -> {}
        }
    }

    /**
     * Display an error if the current password is valid or if there was an error, and
     * otherwise, reset the master password.
     */
    private fun handleReceiveValidatePasswordResult(
        action: ResetPasswordAction.Internal.ReceiveValidatePasswordResult,
    ) {
        when (action.result) {
            // Display an alert if there was an error.
            ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is ValidatePasswordResult.Success -> {
                // Display an error dialog if the password is invalid.
                if (!action.result.isValid) {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = ResetPasswordState.DialogState.Error(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.invalid_master_password.asText(),
                            ),
                        )
                    }
                } else {
                    resetPassword()
                }
            }
        }
    }

    /**
     * Display an alert if the password doesn't meet the policy requirements, then check that
     * the new password matches the retyped password and that the current password is valid.
     */
    private fun handleReceiveValidatePasswordAgainstPoliciesResult(
        action: ResetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult,
    ) {
        // Display an error alert if the new password doesn't meet the policy requirements.
        if (!action.meetsRequirements) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = ResetPasswordState.DialogState.Error(
                        title = R.string.master_password_policy_validation_title.asText(),
                        message = R.string.master_password_policy_validation_message.asText(),
                    ),
                )
            }
            return
        }

        // Check that the re-typed password matches.
        if (!checkRetypedPassword()) return

        // Check that the entered current password is correct.
        viewModelScope.launch {
            val currentPassword = state.currentPasswordInput
            val result = authRepository.validatePassword(currentPassword)
            trySendAction(ResetPasswordAction.Internal.ReceiveValidatePasswordResult(result))
        }
    }

    /**
     * A helper function to determine if the re-typed password matches and
     * display an alert if not. Returns true if the passwords match.
     */
    private fun checkRetypedPassword(): Boolean {
        if (state.passwordInput == state.retypePasswordInput) return true

        mutableStateFlow.update {
            it.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = R.string.an_error_has_occurred.asText(),
                    message = R.string.master_password_confirmation_val_message.asText(),
                ),
            )
        }
        return false
    }

    /**
     * A helper function to launch the reset password request.
     */
    private fun resetPassword() {
        // Show the loading dialog.
        mutableStateFlow.update {
            it.copy(
                dialogState = ResetPasswordState.DialogState.Loading(
                    message = R.string.updating_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.resetPassword(
                currentPassword = state.currentPasswordInput.orNullIfBlank(),
                newPassword = state.passwordInput,
                passwordHint = state.passwordHintInput,
            )
            trySendAction(
                ResetPasswordAction.Internal.ReceiveResetPasswordResult(result),
            )
        }
    }
}

/**
 * Models state of the Reset Password screen.
 */
@Parcelize
data class ResetPasswordState(
    val policies: List<Text>,
    val resetReason: ForcePasswordResetReason?,
    val dialogState: DialogState?,
    val currentPasswordInput: String,
    val passwordInput: String,
    val retypePasswordInput: String,
    val passwordHintInput: String,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. If no title
         * is specified a default will be provided.
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the Reset Password screen.
 */
sealed class ResetPasswordEvent

/**
 * Models actions for the Reset Password screen.
 */
sealed class ResetPasswordAction {
    /**
     * Indicates that the user has confirmed logging out.
     */
    data object ConfirmLogoutClick : ResetPasswordAction()

    /**
     * Indicates that the user has clicked the submit button.
     */
    data object SubmitClick : ResetPasswordAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : ResetPasswordAction()

    /**
     * Indicates that the current password input has changed.
     */
    data class CurrentPasswordInputChanged(val input: String) : ResetPasswordAction()

    /**
     * Indicates that the new password input has changed.
     */
    data class PasswordInputChanged(val input: String) : ResetPasswordAction()

    /**
     * Indicates that the re-type password input has changed.
     */
    data class RetypePasswordInputChanged(val input: String) : ResetPasswordAction()

    /**
     * Indicates that the password hint input has changed.
     */
    data class PasswordHintInputChanged(val input: String) : ResetPasswordAction()

    /**
     * Models actions that the [ResetPasswordViewModel] might send itself.
     */
    sealed class Internal : ResetPasswordAction() {
        /**
         * Indicates that a reset password result has been received.
         */
        data class ReceiveResetPasswordResult(
            val result: ResetPasswordResult,
        ) : Internal()

        /**
         * Indicates that a validate password result has been received.
         */
        data class ReceiveValidatePasswordResult(
            val result: ValidatePasswordResult,
        ) : Internal()

        /**
         * Indicates that a validate password against policies result has been received.
         */
        data class ReceiveValidatePasswordAgainstPoliciesResult(
            val meetsRequirements: Boolean,
        ) : Internal()
    }
}
