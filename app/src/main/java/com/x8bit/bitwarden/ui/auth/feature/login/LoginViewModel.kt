package com.x8bit.bitwarden.ui.auth.feature.login

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import com.x8bit.bitwarden.data.auth.datasource.network.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.datasource.network.util.generateIntentForCaptcha
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial login screen.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginState, LoginEvent, LoginAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginState(
            emailAddress = LoginArgs(savedStateHandle).emailAddress,
            isLoginButtonEnabled = true,
            passwordInput = "",
        ),
) {

    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        authRepository.captchaTokenResultFlow
            .onEach {
                sendAction(
                    LoginAction.Internal.ReceiveCaptchaToken(
                        tokenResult = it,
                    ),
                )
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LoginAction) {
        when (action) {
            LoginAction.LoginButtonClick -> handleLoginButtonClicked()
            LoginAction.NotYouButtonClick -> handleNotYouButtonClicked()
            LoginAction.SingleSignOnClick -> handleSingleSignOnClicked()
            is LoginAction.PasswordInputChanged -> handlePasswordInputChanged(action)
            is LoginAction.Internal.ReceiveCaptchaToken -> {
                handleCaptchaTokenReceived(action.tokenResult)
            }
        }
    }

    private fun handleCaptchaTokenReceived(tokenResult: CaptchaCallbackTokenResult) {
        when (tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                sendEvent(LoginEvent.ShowErrorDialog(messageRes = R.string.captcha_failed))
            }

            is CaptchaCallbackTokenResult.Success -> attemptLogin(captchaToken = tokenResult.token)
        }
    }

    private fun handleLoginButtonClicked() {
        attemptLogin(captchaToken = null)
    }

    private fun attemptLogin(captchaToken: String?) {
        viewModelScope.launch {
            // TODO: show progress here BIT-320
            val result = authRepository.login(
                email = mutableStateFlow.value.emailAddress,
                password = mutableStateFlow.value.passwordInput,
                captchaToken = captchaToken,
            )
            when (result) {
                // TODO: show an error here BIT-320
                LoginResult.Error -> Unit
                // No action required on success, root nav will navigate to logged in state
                LoginResult.Success -> Unit
                is LoginResult.CaptchaRequired -> {
                    sendEvent(
                        event = LoginEvent.NavigateToCaptcha(
                            intent = result.generateIntentForCaptcha(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleNotYouButtonClicked() {
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleSingleSignOnClicked() {
        // TODO BIT-204 navigate to single sign on
    }

    private fun handlePasswordInputChanged(action: LoginAction.PasswordInputChanged) {
        mutableStateFlow.update { it.copy(passwordInput = action.input) }
    }
}

/**
 * Models state of the login screen.
 */
@Parcelize
data class LoginState(
    val passwordInput: String,
    val emailAddress: String,
    val isLoginButtonEnabled: Boolean,
) : Parcelable

/**
 * Models events for the login screen.
 */
sealed class LoginEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : LoginEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val intent: Intent) : LoginEvent()

    /**
     * Shows an error pop up with a given message
     */
    data class ShowErrorDialog(@StringRes val messageRes: Int) : LoginEvent()
}

/**
 * Models actions for the login screen.
 */
sealed class LoginAction {
    /**
     * Indicates that the Login button has been clicked.
     */
    data object LoginButtonClick : LoginAction()

    /**
     * Indicates that the "Not you?" text was clicked.
     */
    data object NotYouButtonClick : LoginAction()

    /**
     * Indicates that the Enterprise single sign-on button has been clicked.
     */
    data object SingleSignOnClick : LoginAction()

    /**
     * Indicates that the password input has changed.
     */
    data class PasswordInputChanged(val input: String) : LoginAction()

    /**
     * Models actions that the [LoginViewModel] itself might send.
     */
    sealed class Internal : LoginAction() {
        /**
         * Indicates a captcha callback token has been received.
         */
        data class ReceiveCaptchaToken(
            val tokenResult: CaptchaCallbackTokenResult,
        ) : Internal()
    }
}
