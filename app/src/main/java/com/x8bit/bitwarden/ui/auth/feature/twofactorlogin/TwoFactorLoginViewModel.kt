package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.util.availableAuthMethods
import com.x8bit.bitwarden.data.auth.datasource.network.util.preferredAuthMethod
import com.x8bit.bitwarden.data.auth.datasource.network.util.twoFactorDisplayEmail
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Two-Factor Login screen.
 */
@HiltViewModel
class TwoFactorLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TwoFactorLoginState, TwoFactorLoginEvent, TwoFactorLoginAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: TwoFactorLoginState(
            authMethod = authRepository.twoFactorData.preferredAuthMethod,
            availableAuthMethods = authRepository.twoFactorData.availableAuthMethods,
            codeInput = "",
            displayEmail = authRepository.twoFactorData.twoFactorDisplayEmail,
            isContinueButtonEnabled = false,
            isRememberMeEnabled = false,
        ),
) {
    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: TwoFactorLoginAction) {
        when (action) {
            TwoFactorLoginAction.CloseButtonClick -> handleCloseButtonClicked()
            is TwoFactorLoginAction.CodeInputChanged -> handleCodeInputChanged(action)
            TwoFactorLoginAction.ContinueButtonClick -> handleContinueButtonClick()
            is TwoFactorLoginAction.RememberMeToggle -> handleRememberMeToggle(action)
            TwoFactorLoginAction.ResendEmailClick -> handleResendEmailClick()
            is TwoFactorLoginAction.SelectAuthMethod -> handleSelectAuthMethod(action)
        }
    }

    /**
     * Update the state with the new text and enable or disable the continue button.
     */
    private fun handleCodeInputChanged(action: TwoFactorLoginAction.CodeInputChanged) {
        mutableStateFlow.update {
            it.copy(
                codeInput = action.input,
                isContinueButtonEnabled = action.input.length >= 6,
            )
        }
    }

    /**
     * Verify the input and attempt to authenticate with the code.
     */
    private fun handleContinueButtonClick() {
        // TODO: Finish implementation (BIT-918)
        sendEvent(TwoFactorLoginEvent.ShowToast("Not yet implemented"))
    }

    /**
     * Dismiss the view.
     */
    private fun handleCloseButtonClicked() {
        sendEvent(TwoFactorLoginEvent.NavigateBack)
    }

    /**
     * Update the state with the new toggle value.
     */
    private fun handleRememberMeToggle(action: TwoFactorLoginAction.RememberMeToggle) {
        mutableStateFlow.update {
            it.copy(
                isRememberMeEnabled = action.isChecked,
            )
        }
    }

    /**
     * Resend the verification code email.
     */
    private fun handleResendEmailClick() {
        // TODO: Finish implementation (BIT-918)
        sendEvent(TwoFactorLoginEvent.ShowToast("Not yet implemented"))
    }

    /**
     * Update the state with the auth method or opens the url for the recovery code.
     */
    private fun handleSelectAuthMethod(action: TwoFactorLoginAction.SelectAuthMethod) {
        if (action.authMethod == TwoFactorAuthMethod.RECOVERY_CODE) {
            sendEvent(TwoFactorLoginEvent.NavigateToRecoveryCode)
        } else {
            mutableStateFlow.update {
                it.copy(
                    authMethod = action.authMethod,
                )
            }
        }
    }
}

/**
 * Models state of the Two-Factor Login screen.
 */
@Parcelize
data class TwoFactorLoginState(
    val authMethod: TwoFactorAuthMethod,
    val availableAuthMethods: List<TwoFactorAuthMethod>,
    val codeInput: String,
    val displayEmail: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
) : Parcelable

/**
 * Models events for the Two-Factor Login screen.
 */
sealed class TwoFactorLoginEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : TwoFactorLoginEvent()

    /**
     * Navigates to the recovery code help page.
     */
    data object NavigateToRecoveryCode : TwoFactorLoginEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : TwoFactorLoginEvent()
}

/**
 * Models actions for the Two-Factor Login screen.
 */
sealed class TwoFactorLoginAction {

    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : TwoFactorLoginAction()

    /**
     * Indicates that the input on the verification code field changed.
     */
    data class CodeInputChanged(
        val input: String,
    ) : TwoFactorLoginAction()

    /**
     * Indicates that the Continue button was clicked.
     */
    data object ContinueButtonClick : TwoFactorLoginAction()

    /**
     * Indicates that the Remember Me switch  toggled.
     */
    data class RememberMeToggle(
        val isChecked: Boolean,
    ) : TwoFactorLoginAction()

    /**
     * Indicates that the Resend Email button was clicked.
     */
    data object ResendEmailClick : TwoFactorLoginAction()

    /**
     * Indicates an auth method was selected from the menu dropdown.
     */
    data class SelectAuthMethod(
        val authMethod: TwoFactorAuthMethod,
    ) : TwoFactorLoginAction()
}
