@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
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
    private val environmentRepository: EnvironmentRepository,
    private val vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginState, LoginEvent, LoginAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginState(
            emailAddress = LoginArgs(savedStateHandle).emailAddress,
            isLoginButtonEnabled = true,
            passwordInput = "",
            environmentLabel = environmentRepository.environment.label,
            loadingDialogState = LoadingDialogState.Hidden,
            errorDialogState = BasicDialogState.Hidden,
            captchaToken = LoginArgs(savedStateHandle).captchaToken,
            accountSummaries = authRepository.userStateFlow.value?.toAccountSummaries().orEmpty(),
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
            LoginAction.AddAccountClick -> handleAddAccountClicked()
            is LoginAction.LockAccountClick -> handleLockAccountClicked(action)
            is LoginAction.LogoutAccountClick -> handleLogoutAccountClicked(action)
            is LoginAction.SwitchAccountClick -> handleSwitchAccountClicked(action)
            is LoginAction.CloseButtonClick -> handleCloseButtonClicked()
            LoginAction.LoginButtonClick -> handleLoginButtonClicked()
            LoginAction.MasterPasswordHintClick -> handleMasterPasswordHintClicked()
            LoginAction.NotYouButtonClick -> handleNotYouButtonClicked()
            LoginAction.SingleSignOnClick -> handleSingleSignOnClicked()
            is LoginAction.PasswordInputChanged -> handlePasswordInputChanged(action)
            is LoginAction.ErrorDialogDismiss -> handleErrorDialogDismiss()
            is LoginAction.Internal.ReceiveCaptchaToken -> {
                handleCaptchaTokenReceived(action.tokenResult)
            }

            is LoginAction.Internal.ReceiveLoginResult -> {
                handleReceiveLoginResult(action = action)
            }
        }
    }

    private fun handleAddAccountClicked() {
        // Since we are already in the login flow we can just go back to the Landing Screen
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleLockAccountClicked(action: LoginAction.LockAccountClick) {
        vaultRepository.lockVaultIfNecessary(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClicked(action: LoginAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
    }

    private fun handleSwitchAccountClicked(action: LoginAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleReceiveLoginResult(action: LoginAction.Internal.ReceiveLoginResult) {
        when (val loginResult = action.loginResult) {
            is LoginResult.CaptchaRequired -> {
                mutableStateFlow.update { it.copy(loadingDialogState = LoadingDialogState.Hidden) }
                sendEvent(
                    event = LoginEvent.NavigateToCaptcha(
                        uri = generateUriForCaptcha(captchaId = loginResult.captchaId),
                    ),
                )
            }

            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        errorDialogState = BasicDialogState.Shown(
                            title = R.string.an_error_has_occurred.asText(),
                            message = (loginResult.errorMessage)?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                        loadingDialogState = LoadingDialogState.Hidden,
                    )
                }
            }

            is LoginResult.Success -> {
                mutableStateFlow.update { it.copy(loadingDialogState = LoadingDialogState.Hidden) }
            }
        }
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update { it.copy(errorDialogState = BasicDialogState.Hidden) }
    }

    private fun handleCaptchaTokenReceived(tokenResult: CaptchaCallbackTokenResult) {
        when (tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        errorDialogState = BasicDialogState.Shown(
                            title = R.string.log_in_denied.asText(),
                            message = R.string.captcha_failed.asText(),
                        ),
                    )
                }
            }

            is CaptchaCallbackTokenResult.Success -> {
                mutableStateFlow.update {
                    it.copy(captchaToken = tokenResult.token)
                }
                attemptLogin()
            }
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleLoginButtonClicked() {
        attemptLogin()
    }

    private fun attemptLogin() {
        mutableStateFlow.update {
            it.copy(
                loadingDialogState = LoadingDialogState.Shown(
                    text = R.string.logging_in.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.login(
                email = mutableStateFlow.value.emailAddress,
                password = mutableStateFlow.value.passwordInput,
                captchaToken = mutableStateFlow.value.captchaToken,
            )
            sendAction(
                LoginAction.Internal.ReceiveLoginResult(
                    loginResult = result,
                ),
            )
        }
    }

    private fun handleMasterPasswordHintClicked() {
        // TODO: Navigate to master password hint screen (BIT-72)
        sendEvent(LoginEvent.ShowToast("Not yet implemented."))
    }

    private fun handleNotYouButtonClicked() {
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleSingleSignOnClicked() {
        // TODO BIT-204 navigate to single sign on
        sendEvent(LoginEvent.ShowToast("Not yet implemented."))
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
    val captchaToken: String?,
    val environmentLabel: String,
    val isLoginButtonEnabled: Boolean,
    val loadingDialogState: LoadingDialogState,
    val errorDialogState: BasicDialogState,
    val accountSummaries: List<AccountSummary>,
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
    data class NavigateToCaptcha(val uri: Uri) : LoginEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: String) : LoginEvent()
}

/**
 * Models actions for the login screen.
 */
sealed class LoginAction {

    /**
     * The user has clicked the add account button.
     */
    data object AddAccountClick : LoginAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to lock
     * the associated account's vault.
     */
    data class LockAccountClick(
        val accountSummary: AccountSummary,
    ) : LoginAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to log out
     * of that account.
     */
    data class LogoutAccountClick(
        val accountSummary: AccountSummary,
    ) : LoginAction()

    /**
     * Indicates the user has clicked on the given [accountSummary] information in order to switch
     * to it.
     */
    data class SwitchAccountClick(
        val accountSummary: AccountSummary,
    ) : LoginAction()

    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : LoginAction()

    /**
     * Indicates that the Login button has been clicked.
     */
    data object LoginButtonClick : LoginAction()

    /**
     * Indicates that the "Not you?" text was clicked.
     */
    data object NotYouButtonClick : LoginAction()

    /**
     * Indicates that the overflow option for getting a master password hint has been clicked.
     */
    data object MasterPasswordHintClick : LoginAction()

    /**
     * Indicates that the Enterprise single sign-on button has been clicked.
     */
    data object SingleSignOnClick : LoginAction()

    /**
     * Indicates that the error dialog has been dismissed.
     */
    data object ErrorDialogDismiss : LoginAction()

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

        /**
         * Indicates a login result has been received.
         */
        data class ReceiveLoginResult(
            val loginResult: LoginResult,
        ) : Internal()
    }
}
