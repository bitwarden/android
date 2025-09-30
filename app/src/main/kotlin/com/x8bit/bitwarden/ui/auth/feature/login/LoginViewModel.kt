@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.auth.feature.login

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.KnownDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toAccountSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val environmentRepository: EnvironmentRepository,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<LoginState, LoginEvent, LoginAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toLoginArgs()
            LoginState(
                emailAddress = args.emailAddress,
                isLoginButtonEnabled = false,
                passwordInput = "",
                environmentLabel = environmentRepository.environment.label,
                dialogState = LoginState.DialogState.Loading(BitwardenString.loading.asText()),
                accountSummaries = authRepository
                    .userStateFlow
                    .value
                    ?.toAccountSummaries()
                    .orEmpty(),
                shouldShowLoginWithDevice = false,
            )
        },
) {

    init {
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
        vaultRepository.lockVault(userId = action.accountSummary.userId, isUserInitiated = true)
    }

    private fun handleLogoutAccountClicked(action: LoginAction.LogoutAccountClick) {
        authRepository.logout(
            userId = action.accountSummary.userId,
            reason = LogoutReason.Click(source = "LoginViewModel"),
        )
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

    @Suppress("MaxLineLength", "LongMethod")
    private fun handleReceiveLoginResult(action: LoginAction.Internal.ReceiveLoginResult) {
        when (val loginResult = action.loginResult) {
            is LoginResult.EncryptionKeyMigrationRequired -> {
                val vaultUrl =
                    environmentRepository
                        .environment
                        .environmentUrlData
                        .baseWebVaultUrlOrDefault

                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString
                                .this_account_will_soon_be_deleted_log_in_at_x_to_continue_using_bitwarden
                                .asText(vaultUrl.toUriOrNull()?.host ?: vaultUrl),
                        ),
                    )
                }
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

            // NO-OP: This result should not be possible here
            is LoginResult.ConfirmKeyConnectorDomain -> Unit

            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = loginResult.errorMessage?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            error = loginResult.error,
                        ),
                    )
                }
            }

            LoginResult.UnofficialServerError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server.asText(),
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
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    )
                }
            }

            is LoginResult.NewDeviceVerification -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    LoginEvent.NavigateToTwoFactorLogin(
                        emailAddress = state.emailAddress,
                        password = state.passwordInput,
                        isNewDeviceVerification = true,
                    ),
                )
            }
        }
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
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
                    message = BitwardenString.logging_in.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.login(
                email = state.emailAddress,
                password = state.passwordInput,
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
        val isNewDeviceVerification: Boolean = false,
    ) : LoginEvent()
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
