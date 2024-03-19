package com.x8bit.bitwarden.ui.auth.feature.setpassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.SetPasswordResult
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.util.toDisplayLabels
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val MIN_PASSWORD_LENGTH = 12

/**
 * Manages application state for the Set Password screen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class SetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SetPasswordState, SetPasswordEvent, SetPasswordAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val organizationIdentifier = authRepository.ssoOrganizationIdentifier
        if (organizationIdentifier.isNullOrBlank()) authRepository.logout()
        SetPasswordState(
            dialogState = null,
            organizationIdentifier = organizationIdentifier.orEmpty(),
            passwordInput = "",
            passwordHintInput = "",
            policies = authRepository.passwordPolicies.toDisplayLabels(),
            retypePasswordInput = "",
        )
    },
) {
    override fun handleAction(action: SetPasswordAction) {
        when (action) {
            SetPasswordAction.CancelClick -> handleCancelClick()
            SetPasswordAction.SubmitClick -> handleSubmitClicked()
            SetPasswordAction.DialogDismiss -> handleDialogDismiss()

            is SetPasswordAction.PasswordInputChanged -> handlePasswordInputChanged(action)

            is SetPasswordAction.RetypePasswordInputChanged -> {
                handleRetypePasswordInputChanged(action)
            }

            is SetPasswordAction.PasswordHintInputChanged -> {
                handlePasswordHintInputChanged(action)
            }

            is SetPasswordAction.Internal.ReceiveSetPasswordResult -> {
                handleReceiveSetPasswordResult(action)
            }

            is SetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult -> {
                handleReceiveValidatePasswordAgainstPoliciesResult(action)
            }
        }
    }

    /**
     * Dismiss the view if the user cancels the set master password functionality.
     */
    private fun handleCancelClick() {
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
                    dialogState = SetPasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required
                            .asText(R.string.master_password.asText()),
                    ),
                )
            }
            return
        }

        // Validate password against policies if there are any.
        if (state.policies.isNotEmpty()) {
            viewModelScope.launch {
                sendAction(
                    SetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult(
                        authRepository.validatePasswordAgainstPolicies(state.passwordInput),
                    ),
                )
            }
        } else if (state.passwordInput.length < MIN_PASSWORD_LENGTH) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SetPasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.master_password_length_val_message_x
                            .asText(MIN_PASSWORD_LENGTH),
                    ),
                )
            }
        } else if (state.passwordInput == state.retypePasswordInput) {
            setPassword()
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SetPasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.master_password_confirmation_val_message.asText(),
                    ),
                )
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
     * Update the state with the new master password input.
     */
    private fun handlePasswordInputChanged(action: SetPasswordAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(
                passwordInput = action.input,
            )
        }
    }

    /**
     * Update the state with the re-typed master password input.
     */
    private fun handleRetypePasswordInputChanged(
        action: SetPasswordAction.RetypePasswordInputChanged,
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
        action: SetPasswordAction.PasswordHintInputChanged,
    ) {
        mutableStateFlow.update {
            it.copy(
                passwordHintInput = action.input,
            )
        }
    }

    /**
     * Show an alert if the set password attempt failed, otherwise attempt to unlock the vault.
     */
    private fun handleReceiveSetPasswordResult(
        action: SetPasswordAction.Internal.ReceiveSetPasswordResult,
    ) {
        when (action.result) {
            SetPasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SetPasswordState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            SetPasswordResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }
        }
    }

    /**
     * Display an alert if the password doesn't meet the policy requirements, then check that
     * the new password matches the retyped password and that the current password is valid.
     */
    private fun handleReceiveValidatePasswordAgainstPoliciesResult(
        action: SetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult,
    ) {
        // Display an error alert if the new password doesn't meet the policy requirements.
        if (!action.meetsRequirements) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SetPasswordState.DialogState.Error(
                        title = R.string.master_password_policy_validation_title.asText(),
                        message = R.string.master_password_policy_validation_message.asText(),
                    ),
                )
            }
        }
    }

    /**
     * A helper function to launch the set password request.
     */
    private fun setPassword() {
        // Show the loading dialog.
        mutableStateFlow.update {
            it.copy(
                dialogState = SetPasswordState.DialogState.Loading(
                    message = R.string.updating_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            sendAction(
                SetPasswordAction.Internal.ReceiveSetPasswordResult(
                    result = authRepository.setPassword(
                        organizationIdentifier = state.organizationIdentifier,
                        password = state.passwordInput,
                        passwordHint = state.passwordHintInput,
                    ),
                ),
            )
        }
    }
}

/**
 * Models state of the Set Password screen.
 */
@Parcelize
data class SetPasswordState(
    val dialogState: DialogState?,
    val organizationIdentifier: String,
    val passwordHintInput: String,
    val passwordInput: String,
    val policies: List<Text>,
    val retypePasswordInput: String,
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
            val title: Text? = null,
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
 * Models events for the Set Password screen.
 */
sealed class SetPasswordEvent

/**
 * Models actions for the Set Password screen.
 */
sealed class SetPasswordAction {
    /**
     * Indicates that the user has confirmed logging out.
     */
    data object CancelClick : SetPasswordAction()

    /**
     * Indicates that the user has clicked the submit button.
     */
    data object SubmitClick : SetPasswordAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : SetPasswordAction()

    /**
     * Indicates that the master password input has changed.
     */
    data class PasswordInputChanged(val input: String) : SetPasswordAction()

    /**
     * Indicates that the re-type master password input has changed.
     */
    data class RetypePasswordInputChanged(val input: String) : SetPasswordAction()

    /**
     * Indicates that the password hint input has changed.
     */
    data class PasswordHintInputChanged(val input: String) : SetPasswordAction()

    /**
     * Models actions that the [SetPasswordViewModel] might send itself.
     */
    sealed class Internal : SetPasswordAction() {
        /**
         * Indicates that a set password result has been received.
         */
        data class ReceiveSetPasswordResult(
            val result: SetPasswordResult,
        ) : Internal()

        /**
         * Indicates that a validate password against policies result has been received.
         */
        data class ReceiveValidatePasswordAgainstPoliciesResult(
            val meetsRequirements: Boolean,
        ) : Internal()
    }
}
