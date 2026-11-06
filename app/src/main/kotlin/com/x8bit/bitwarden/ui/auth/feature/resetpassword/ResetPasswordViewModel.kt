package com.x8bit.bitwarden.ui.auth.feature.resetpassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.datasource.disk.model.ForcePasswordResetReason
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.PasswordStrengthResult
import com.x8bit.bitwarden.data.auth.repository.model.ResetPasswordResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.auth.feature.completeregistration.PasswordStrengthState
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.util.toDisplayLabels
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toStrictestPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
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
    private val vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ResetPasswordState, ResetPasswordEvent, ResetPasswordAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: ResetPasswordState(
        viewState = ResetPasswordState.ViewState.Loading,
        dialogState = null,
    ),
) {
    init {
        syncVault()
    }

    override fun handleAction(action: ResetPasswordAction) {
        when (action) {
            ResetPasswordAction.ConfirmLogoutClick -> handleConfirmLogoutClick()
            ResetPasswordAction.SaveClick -> handleSaveClicked()
            ResetPasswordAction.DialogDismiss -> handleDialogDismiss()
            ResetPasswordAction.RetryClick -> handleRetryClick()
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

            is ResetPasswordAction.Internal.ReceiveSyncResult -> handleReceiveSyncResult(action)
            is ResetPasswordAction.Internal.ReceiveResetPasswordResult -> {
                handleReceiveResetPasswordResult(action)
            }

            is ResetPasswordAction.Internal.ReceiveValidatePasswordAgainstPoliciesResult -> {
                handleReceiveValidatePasswordAgainstPoliciesResult(action)
            }

            is ResetPasswordAction.Internal.ReceiveValidatePasswordResult -> {
                handleReceiveValidatePasswordResult(action)
            }

            ResetPasswordAction.LearnHowPreventLockoutClick -> {
                handleLearnHowToPreventLockoutClick()
            }
        }
    }

    private fun handleRetryClick() {
        mutableStateFlow.update {
            it.copy(viewState = ResetPasswordState.ViewState.Loading)
        }
        syncVault()
    }

    /**
     * Display the content if the sync succeeded, refreshing the policies in case they changed,
     * and otherwise display an error.
     */
    private fun handleReceiveSyncResult(action: ResetPasswordAction.Internal.ReceiveSyncResult) {
        when (action.result) {
            is SyncVaultDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ResetPasswordState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is SyncVaultDataResult.Success -> {
                val policies = authRepository.passwordPolicies
                mutableStateFlow.update {
                    it.copy(
                        viewState = ResetPasswordState.ViewState.Content(
                            resetReason = authRepository.passwordResetReason,
                            policies = policies.toDisplayLabels(),
                            minimumPasswordLength = policies
                                .toStrictestPolicy()
                                .minLength
                                ?: MIN_PASSWORD_LENGTH,
                            currentPasswordInput = "",
                            passwordInput = "",
                            retypePasswordInput = "",
                            passwordHintInput = "",
                            passwordStrengthState = PasswordStrengthState.NONE,
                        ),
                    )
                }
            }
        }
    }

    private fun handleLearnHowToPreventLockoutClick() {
        sendEvent(ResetPasswordEvent.NavigateToPreventAccountLockout)
    }

    /**
     * Dismiss the view if the user confirms logging out.
     */
    private fun handleConfirmLogoutClick() {
        authRepository.logout(reason = LogoutReason.Click(source = "ResetPasswordViewModel"))
    }

    /**
     * Validate the user's current password when they submit.
     */
    private fun handleSaveClicked() {
        onContent { content ->
            // Display an error dialog if the new password field is blank.
            if (content.passwordInput.isBlank()) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString
                                .validation_field_required
                                .asText(BitwardenString.master_password.asText()),
                        ),
                    )
                }
                return@onContent
            }

            when (content.resetReason) {
                ForcePasswordResetReason.WEAK_MASTER_PASSWORD_ON_LOGIN -> {
                    // Check if the new password meets the policy requirements, if applicable.
                    viewModelScope.launch {
                        val result = authRepository.validatePasswordAgainstPolicies(
                            password = content.passwordInput,
                        )
                        sendAction(
                            ResetPasswordAction
                                .Internal
                                .ReceiveValidatePasswordAgainstPoliciesResult(
                                    meetsRequirements = result,
                                ),
                        )
                    }
                }

                ForcePasswordResetReason.ADMIN_FORCE_PASSWORD_RESET,
                ForcePasswordResetReason.TDE_USER_WITHOUT_PASSWORD_HAS_PASSWORD_RESET_PERMISSION,
                null,
                    -> {
                    // Otherwise, simply verify that the password meets the
                    // minimum length requirement.
                    if (content.passwordInput.length < MIN_PASSWORD_LENGTH) {
                        mutableStateFlow.update {
                            it.copy(
                                dialogState = ResetPasswordState.DialogState.Error(
                                    title = BitwardenString.an_error_has_occurred.asText(),
                                    message = BitwardenPlurals
                                        .master_password_length_val_message_x
                                        .asPluralsText(
                                            quantity = MIN_PASSWORD_LENGTH,
                                            args = arrayOf(MIN_PASSWORD_LENGTH),
                                        ),
                                ),
                            )
                        }
                    } else {
                        // Check that the re-typed password matches.
                        if (!checkRetypedPassword(content = content)) return@onContent

                        // Otherwise, if the password checks out, attempt to reset it.
                        resetPassword()
                    }
                }
            }
        }
    }

    /**
     * Dismiss the dialog state.
     */
    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    /**
     * Update the state with the current password input.
     */
    private fun handleCurrentPasswordInputChanged(
        action: ResetPasswordAction.CurrentPasswordInputChanged,
    ) {
        updateContent {
            it.copy(currentPasswordInput = action.input)
        }
    }

    /**
     * Update the state with the new password input.
     */
    private fun handlePasswordInputChanged(action: ResetPasswordAction.PasswordInputChanged) {
        updateContent { content ->
            val input = action.input
            if (input.isEmpty()) {
                content.copy(
                    passwordStrengthState = PasswordStrengthState.NONE,
                    passwordInput = input,
                )
            } else {
                when (val result = authRepository.getPasswordStrength(password = input)) {
                    is PasswordStrengthResult.Success -> {
                        content.copy(
                            passwordStrengthState = when (result.passwordStrength) {
                                PasswordStrength.LEVEL_0 -> PasswordStrengthState.WEAK_1
                                PasswordStrength.LEVEL_1 -> PasswordStrengthState.WEAK_2
                                PasswordStrength.LEVEL_2 -> PasswordStrengthState.WEAK_3
                                PasswordStrength.LEVEL_3 -> PasswordStrengthState.GOOD
                                PasswordStrength.LEVEL_4 -> PasswordStrengthState.STRONG
                            },
                            passwordInput = input,
                        )
                    }

                    is PasswordStrengthResult.Error -> content.copy(passwordInput = input)
                }
            }
        }
    }

    /**
     * Update the state with the re-typed password input.
     */
    private fun handleRetypePasswordInputChanged(
        action: ResetPasswordAction.RetypePasswordInputChanged,
    ) {
        updateContent {
            it.copy(retypePasswordInput = action.input)
        }
    }

    /**
     * Update the state with the password hint input.
     */
    private fun handlePasswordHintInputChanged(
        action: ResetPasswordAction.PasswordHintInputChanged,
    ) {
        updateContent {
            it.copy(passwordHintInput = action.input)
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

        when (val result = action.result) {
            // Display an alert if there was an error.
            is ResetPasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
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
        when (val result = action.result) {
            // Display an alert if there was an error.
            is ValidatePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = ResetPasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
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
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.invalid_master_password.asText(),
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
                        title = BitwardenString.master_password_policy_validation_title.asText(),
                        message = BitwardenString.master_password_policy_validation_message
                            .asText(),
                    ),
                )
            }
            return
        }

        onContent { content ->
            // Check that the re-typed password matches.
            if (!checkRetypedPassword(content = content)) return@onContent

            // Check that the entered current password is correct.
            viewModelScope.launch {
                val result = authRepository.validatePassword(content.currentPasswordInput)
                trySendAction(ResetPasswordAction.Internal.ReceiveValidatePasswordResult(result))
            }
        }
    }

    /**
     * A helper function to determine if the re-typed password matches and
     * display an alert if not. Returns true if the passwords match.
     */
    private fun checkRetypedPassword(
        content: ResetPasswordState.ViewState.Content,
    ): Boolean {
        if (content.passwordInput == content.retypePasswordInput) return true

        mutableStateFlow.update {
            it.copy(
                dialogState = ResetPasswordState.DialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.master_password_confirmation_val_message.asText(),
                ),
            )
        }
        return false
    }

    /**
     * A helper function to launch the vault sync request.
     */
    private fun syncVault() {
        viewModelScope.launch {
            val result = vaultRepository.syncForResult()
            sendAction(ResetPasswordAction.Internal.ReceiveSyncResult(result))
        }
    }

    /**
     * A helper function to launch the reset password request.
     */
    private fun resetPassword() {
        onContent { content ->
            // Show the loading dialog.
            mutableStateFlow.update {
                it.copy(
                    dialogState = ResetPasswordState.DialogState.Loading(
                        message = BitwardenString.updating_password.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = authRepository.resetPassword(
                    currentPassword = content.currentPasswordInput.orNullIfBlank(),
                    newPassword = content.passwordInput,
                    passwordHint = content.passwordHintInput,
                )
                trySendAction(ResetPasswordAction.Internal.ReceiveResetPasswordResult(result))
            }
        }
    }

    private inline fun onContent(
        crossinline block: (ResetPasswordState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? ResetPasswordState.ViewState.Content)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            ResetPasswordState.ViewState.Content,
        ) -> ResetPasswordState.ViewState.Content,
    ) {
        val updatedContent = (state.viewState as? ResetPasswordState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }
}

/**
 * Models state of the Reset Password screen.
 */
@Parcelize
data class ResetPasswordState(
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the specific view states for the Reset Password screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Indicates if the save button should be displayed.
         */
        abstract val shouldShowSaveButton: Boolean

        /**
         * Represents a loading state while the vault is syncing.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val shouldShowSaveButton: Boolean get() = false
        }

        /**
         * Represents an error state with the given [message].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val shouldShowSaveButton: Boolean get() = false
        }

        /**
         * Represents the loaded content state.
         */
        @Parcelize
        data class Content(
            val resetReason: ForcePasswordResetReason?,
            val policies: ImmutableList<Text>,
            val minimumPasswordLength: Int,
            val currentPasswordInput: String,
            val passwordInput: String,
            val retypePasswordInput: String,
            val passwordHintInput: String,
            val passwordStrengthState: PasswordStrengthState,
        ) : ViewState() {
            override val shouldShowSaveButton: Boolean get() = true
        }
    }

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
            val error: Throwable? = null,
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
sealed class ResetPasswordEvent {

    /**
     * Navigate to the LearnToPreventLockout screen
     */
    data object NavigateToPreventAccountLockout : ResetPasswordEvent()
}

/**
 * Models actions for the Reset Password screen.
 */
sealed class ResetPasswordAction {
    /**
     * Indicates that the user has confirmed logging out.
     */
    data object ConfirmLogoutClick : ResetPasswordAction()

    /**
     * Indicates that the user has clicked the save button.
     */
    data object SaveClick : ResetPasswordAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : ResetPasswordAction()

    /**
     * Indicates that the user has clicked the retry button after a failed sync.
     */
    data object RetryClick : ResetPasswordAction()

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
     * Indicates user clicked the "Learn ways..." text
     */
    data object LearnHowPreventLockoutClick : ResetPasswordAction()

    /**
     * Models actions that the [ResetPasswordViewModel] might send itself.
     */
    sealed class Internal : ResetPasswordAction() {
        /**
         * Indicates that a vault sync result has been received.
         */
        data class ReceiveSyncResult(
            val result: SyncVaultDataResult,
        ) : Internal()

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
