@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.auth.feature.login

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial login screen.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    environmentRepository: EnvironmentRepository,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<LoginState, LoginEvent, LoginAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginState(
            emailAddress = LoginArgs(savedStateHandle).emailAddress,
            isLoginButtonEnabled = false,
            passwordInput = "",
            environmentLabel = environmentRepository.environment.label,
            dialogState = LoginState.DialogState.Loading(R.string.loading.asText()),
            captchaToken = LoginArgs(savedStateHandle).captchaToken,
            accountSummaries = authRepository.userStateFlow.value?.toAccountSummaries().orEmpty(),
            shouldShowLoginWithDevice = false,
        ),
) {

    init {
        authRepository.captchaTokenResultFlow
            .onEach {
                sendAction(
                    LoginAction.Internal.ReceiveCaptchaToken(
                        tokenResult = it,
                    ),
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            trySendAction(
                LoginAction.Internal.ReceiveKnownDeviceResult(
                    knownDeviceResult = authRepository.getIsKnownDevice(state.emailAddress),
                ),
            )
        }
    }

    override fun handleAction(action: LoginAction) {
        when (action) {
            LoginAction.AddAccountClick -> handleAddAccountClicked()
            is LoginAction.LockAccountClick -> handleLockAccountClicked(action)
            is LoginAction.LogoutAccountClick -> handleLogoutAccountClicked(action)
            is LoginAction.SwitchAccountClick -> handleSwitchAccountClicked(action)
            is LoginAction.CloseButtonClick -> handleCloseButtonClicked()
            LoginAction.LoginButtonClick -> handleLoginButtonClicked()
            LoginAction.LoginWithDeviceButtonClick -> handleLoginWithDeviceButtonClicked()
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

            is LoginAction.Internal.ReceiveKnownDeviceResult -> {
                handleKnownDeviceResultReceived(action)
            }
        }
    }

    private fun handleAddAccountClicked() {
        // Since we are already in the login flow we can just go back to the Landing Screen
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleLockAccountClicked(action: LoginAction.LockAccountClick) {
        vaultRepository.lockVault(userId = action.accountSummary.userId)
    }

    private fun handleLogoutAccountClicked(action: LoginAction.LogoutAccountClick) {
        authRepository.logout(userId = action.accountSummary.userId)
    }

    private fun handleSwitchAccountClicked(action: LoginAction.SwitchAccountClick) {
        authRepository.switchAccount(userId = action.accountSummary.userId)
    }

    private fun handleKnownDeviceResultReceived(
        action: LoginAction.Internal.ReceiveKnownDeviceResult,
    ) {
        when (action.knownDeviceResult) {
            is KnownDeviceResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        shouldShowLoginWithDevice = action.knownDeviceResult.isKnownDevice,
                    )
                }
            }

            is KnownDeviceResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        shouldShowLoginWithDevice = false,
                    )
                }
            }
        }
    }

    @Suppress("MaxLineLength")
    private fun handleReceiveLoginResult(action: LoginAction.Internal.ReceiveLoginResult) {
        when (val loginResult = action.loginResult) {
            is LoginResult.CaptchaRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    event = LoginEvent.NavigateToCaptcha(
                        uri = generateUriForCaptcha(captchaId = loginResult.captchaId),
                    ),
                )
            }

            is LoginResult.TwoFactorRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    LoginEvent.NavigateToTwoFactorLogin(
                        emailAddress = state.emailAddress,
                        password = state.passwordInput,
                    ),
                )
            }

            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = loginResult.errorMessage?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            LoginResult.UnofficialServerError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
                        ),
                    )
                }
            }

            is LoginResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }

            LoginResult.CertificateError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleCaptchaTokenReceived(tokenResult: CaptchaCallbackTokenResult) {
        when (tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
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

    private fun handleLoginWithDeviceButtonClicked() {
        sendEvent(LoginEvent.NavigateToLoginWithDevice(state.emailAddress))
    }

    private fun attemptLogin() {
        mutableStateFlow.update {
            it.copy(
                dialogState = LoginState.DialogState.Loading(
                    message = R.string.logging_in.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.login(
                email = state.emailAddress,
                password = state.passwordInput,
                captchaToken = state.captchaToken,
            )
            sendAction(
                LoginAction.Internal.ReceiveLoginResult(
                    loginResult = result,
                ),
            )
        }
    }

    private fun handleMasterPasswordHintClicked() {
        val email = state.emailAddress
        sendEvent(LoginEvent.NavigateToMasterPasswordHint(email))
    }

    private fun handleNotYouButtonClicked() {
        sendEvent(LoginEvent.NavigateBack)
    }

    private fun handleSingleSignOnClicked() {
        val email = state.emailAddress
        sendEvent(LoginEvent.NavigateToEnterpriseSignOn(email))
    }

    private fun handlePasswordInputChanged(action: LoginAction.PasswordInputChanged) {
        mutableStateFlow.update {
            it.copy(
                passwordInput = action.input,
                isLoginButtonEnabled = action.input.isNotBlank(),
            )
        }
    }
}

/**
 * Models state of the login screen.
 */
@Parcelize
data class LoginState(
    // We never want this saved since the input is sensitive data.
    @IgnoredOnParcel val passwordInput: String = "",
    val emailAddress: String,
    val captchaToken: String?,
    val environmentLabel: String,
    val isLoginButtonEnabled: Boolean,
    val dialogState: DialogState?,
    val accountSummaries: List<AccountSummary>,
    val shouldShowLoginWithDevice: Boolean,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents a dismissible dialog with the given error [message].
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
 * Models events for the login screen.
 */
sealed class LoginEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : LoginEvent()

    /**
     * Navigate to the master password hit screen.
     */
    data class NavigateToMasterPasswordHint(
        val emailAddress: String,
    ) : LoginEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val uri: Uri) : LoginEvent()

    /**
     * Navigates to the enterprise single sign on screen.
     */
    data class NavigateToEnterpriseSignOn(val emailAddress: String) : LoginEvent()

    /**
     * Navigates to the login with device screen.
     */
    data class NavigateToLoginWithDevice(
        val emailAddress: String,
    ) : LoginEvent()

    /**
     * Navigates to the two-factor login screen.
     */
    data class NavigateToTwoFactorLogin(
        val emailAddress: String,
        val password: String?,
    ) : LoginEvent()

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
     * Indicates that the Login With Device button has been clicked.
     */
    data object LoginWithDeviceButtonClick : LoginAction()

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
         * Indicates that a [KnownDeviceResult] has been received and state should be updated.
         */
        data class ReceiveKnownDeviceResult(
            val knownDeviceResult: KnownDeviceResult,
        ) : Internal()

        /**
         * Indicates a login result has been received.
         */
        data class ReceiveLoginResult(
            val loginResult: LoginResult,
        ) : Internal()
    }
}
