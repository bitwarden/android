package com.x8bit.bitwarden.ui.auth.feature.createaccount

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.AcceptPoliciesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.CheckDataBreachesToggle
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.ConfirmPasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordHintChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PasswordInputChange
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.SubmitClick
import com.x8bit.bitwarden.ui.auth.feature.createaccount.CreateAccountAction.TermsClick
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val MIN_PASSWORD_LENGTH = 12

/**
 * Models logic for the create account screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CreateAccountState, CreateAccountEvent, CreateAccountAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: CreateAccountState(
            emailInput = "",
            passwordInput = "",
            confirmPasswordInput = "",
            passwordHintInput = "",
            isAcceptPoliciesToggled = false,
            isCheckDataBreachesToggled = false,
            errorDialogState = BasicDialogState.Hidden,
        ),
) {

    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CreateAccountAction) {
        when (action) {
            is SubmitClick -> handleSubmitClick()
            is ConfirmPasswordInputChange -> handleConfirmPasswordInputChanged(action)
            is EmailInputChange -> handleEmailInputChanged(action)
            is PasswordHintChange -> handlePasswordHintChanged(action)
            is PasswordInputChange -> handlePasswordInputChanged(action)
            is CreateAccountAction.CloseClick -> handleCloseClick()
            is CreateAccountAction.ErrorDialogDismiss -> handleDialogDismiss()
            is AcceptPoliciesToggle -> handleAcceptPoliciesToggle(action)
            is CheckDataBreachesToggle -> handleCheckDataBreachesToggle(action)
            is PrivacyPolicyClick -> handlePrivacyPolicyClick()
            is TermsClick -> handleTermsClick()
        }
    }

    private fun handlePrivacyPolicyClick() = sendEvent(CreateAccountEvent.NavigateToPrivacyPolicy)

    private fun handleTermsClick() = sendEvent(CreateAccountEvent.NavigateToTerms)

    private fun handleAcceptPoliciesToggle(action: AcceptPoliciesToggle) {
        mutableStateFlow.update {
            it.copy(isAcceptPoliciesToggled = action.newState)
        }
    }

    private fun handleCheckDataBreachesToggle(action: CheckDataBreachesToggle) {
        mutableStateFlow.update {
            it.copy(isCheckDataBreachesToggled = action.newState)
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(errorDialogState = BasicDialogState.Hidden)
        }
    }

    private fun handleCloseClick() {
        sendEvent(CreateAccountEvent.NavigateBack)
    }

    private fun handleEmailInputChanged(action: EmailInputChange) {
        mutableStateFlow.update { it.copy(emailInput = action.input) }
    }

    private fun handlePasswordHintChanged(action: PasswordHintChange) {
        mutableStateFlow.update { it.copy(passwordHintInput = action.input) }
    }

    private fun handlePasswordInputChanged(action: PasswordInputChange) {
        mutableStateFlow.update { it.copy(passwordInput = action.input) }
    }

    private fun handleConfirmPasswordInputChanged(action: ConfirmPasswordInputChange) {
        mutableStateFlow.update { it.copy(confirmPasswordInput = action.input) }
    }

    private fun handleSubmitClick() = when {
        mutableStateFlow.value.passwordInput.length < MIN_PASSWORD_LENGTH -> {
            val dialog = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.master_password_length_val_message_x.asText(MIN_PASSWORD_LENGTH),
            )
            mutableStateFlow.update { it.copy(errorDialogState = dialog) }
        }

        else -> {
            sendEvent(CreateAccountEvent.ShowToast("TODO: Handle Submit Click"))
        }
    }
}

/**
 * UI state for the create account screen.
 */
@Parcelize
data class CreateAccountState(
    val emailInput: String,
    val passwordInput: String,
    val confirmPasswordInput: String,
    val passwordHintInput: String,
    val isCheckDataBreachesToggled: Boolean,
    val isAcceptPoliciesToggled: Boolean,
    val errorDialogState: BasicDialogState,
) : Parcelable

/**
 * Models events for the create account screen.
 */
sealed class CreateAccountEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CreateAccountEvent()

    /**
     * Placeholder event for showing a toast. Can be removed once there are real events.
     */
    data class ShowToast(val text: String) : CreateAccountEvent()

    /**
     * Navigate to terms and conditions.
     */
    data object NavigateToTerms : CreateAccountEvent()

    /**
     * Navigate to privacy policy.
     */
    data object NavigateToPrivacyPolicy : CreateAccountEvent()
}

/**
 * Models actions for the create account screen.
 */
sealed class CreateAccountAction {
    /**
     * User clicked submit.
     */
    data object SubmitClick : CreateAccountAction()

    /**
     * User clicked close.
     */
    data object CloseClick : CreateAccountAction()

    /**
     * Email input changed.
     */
    data class EmailInputChange(val input: String) : CreateAccountAction()

    /**
     * Password input changed.
     */
    data class PasswordInputChange(val input: String) : CreateAccountAction()

    /**
     * Confirm password input changed.
     */
    data class ConfirmPasswordInputChange(val input: String) : CreateAccountAction()

    /**
     * Password hint input changed.
     */
    data class PasswordHintChange(val input: String) : CreateAccountAction()

    /**
     * User dismissed the error dialog.
     */
    data object ErrorDialogDismiss : CreateAccountAction()

    /**
     * User tapped check data breaches toggle.
     */
    data class CheckDataBreachesToggle(val newState: Boolean) : CreateAccountAction()

    /**
     * User tapped accept policies toggle.
     */
    data class AcceptPoliciesToggle(val newState: Boolean) : CreateAccountAction()

    /**
     * User tapped privacy policy link.
     */
    data object PrivacyPolicyClick : CreateAccountAction()

    /**
     * User tapped terms link.
     */
    data object TermsClick : CreateAccountAction()
}
