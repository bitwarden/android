package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.CreateAuthRequestResult
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Login with Device screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class LoginWithDeviceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginWithDeviceState, LoginWithDeviceEvent, LoginWithDeviceAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginWithDeviceState(
            emailAddress = LoginWithDeviceArgs(savedStateHandle).emailAddress,
            viewState = LoginWithDeviceState.ViewState.Loading,
            dialogState = null,
            loginData = null,
        ),
) {
    private var authJob: Job = Job().apply { complete() }

    init {
        sendNewAuthRequest(isResend = false)
        authRepository
            .captchaTokenResultFlow
            .map { LoginWithDeviceAction.Internal.ReceiveCaptchaToken(tokenResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LoginWithDeviceAction) {
        when (action) {
            LoginWithDeviceAction.CloseButtonClick -> handleCloseButtonClicked()
            LoginWithDeviceAction.DismissDialog -> handleErrorDialogDismissed()
            LoginWithDeviceAction.ResendNotificationClick -> handleResendNotificationClicked()
            LoginWithDeviceAction.ViewAllLogInOptionsClick -> handleViewAllLogInOptionsClicked()
            is LoginWithDeviceAction.Internal -> handleInternalActions(action)
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(LoginWithDeviceEvent.NavigateBack)
    }

    private fun handleErrorDialogDismissed() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleResendNotificationClicked() {
        sendNewAuthRequest(isResend = true)
    }

    private fun handleViewAllLogInOptionsClicked() {
        sendEvent(LoginWithDeviceEvent.NavigateBack)
    }

    private fun handleInternalActions(action: LoginWithDeviceAction.Internal) {
        when (action) {
            is LoginWithDeviceAction.Internal.NewAuthRequestResultReceive -> {
                handleNewAuthRequestResultReceived(action)
            }

            is LoginWithDeviceAction.Internal.ReceiveCaptchaToken -> {
                handleReceiveCaptchaToken(action)
            }

            is LoginWithDeviceAction.Internal.ReceiveLoginResult -> {
                handleReceiveLoginResult(action)
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleNewAuthRequestResultReceived(
        action: LoginWithDeviceAction.Internal.NewAuthRequestResultReceive,
    ) {
        when (val result = action.result) {
            is CreateAuthRequestResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = "",
                            isResendNotificationLoading = false,
                        ),
                        dialogState = null,
                        loginData = LoginWithDeviceState.LoginData(
                            accessCode = result.authRequestResponse.accessCode,
                            requestId = result.authRequest.id,
                            masterPasswordHash = result.authRequest.masterPasswordHash,
                            asymmetricalKey = requireNotNull(result.authRequest.key),
                            privateKey = result.authRequestResponse.privateKey,
                            captchaToken = null,
                        ),
                    )
                }
                attemptLogin()
            }

            is CreateAuthRequestResult.Update -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = result.authRequest.fingerprint,
                            isResendNotificationLoading = false,
                        ),
                        dialogState = null,
                    )
                }
            }

            is CreateAuthRequestResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = "",
                            isResendNotificationLoading = false,
                        ),
                        dialogState = LoginWithDeviceState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            CreateAuthRequestResult.Declined -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = "",
                            isResendNotificationLoading = false,
                        ),
                        dialogState = LoginWithDeviceState.DialogState.Error(
                            title = null,
                            message = R.string.this_request_is_no_longer_valid.asText(),
                        ),
                    )
                }
            }

            CreateAuthRequestResult.Expired -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = "",
                            isResendNotificationLoading = false,
                        ),
                        dialogState = LoginWithDeviceState.DialogState.Error(
                            title = null,
                            message = R.string.login_request_has_already_expired.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleReceiveCaptchaToken(
        action: LoginWithDeviceAction.Internal.ReceiveCaptchaToken,
    ) {
        when (val tokenResult = action.tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginWithDeviceState.DialogState.Error(
                            title = R.string.log_in_denied.asText(),
                            message = R.string.captcha_failed.asText(),
                        ),
                    )
                }
            }

            is CaptchaCallbackTokenResult.Success -> {
                mutableStateFlow.update {
                    it.copy(loginData = it.loginData?.copy(captchaToken = tokenResult.token))
                }
                attemptLogin()
            }
        }
    }

    private fun handleReceiveLoginResult(
        action: LoginWithDeviceAction.Internal.ReceiveLoginResult,
    ) {
        when (val loginResult = action.loginResult) {
            is LoginResult.CaptchaRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    event = LoginWithDeviceEvent.NavigateToCaptcha(
                        uri = generateUriForCaptcha(captchaId = loginResult.captchaId),
                    ),
                )
            }

            is LoginResult.TwoFactorRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    LoginWithDeviceEvent.NavigateToTwoFactorLogin(
                        emailAddress = state.emailAddress,
                    ),
                )
            }

            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginWithDeviceState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = loginResult
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is LoginResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }
        }
    }

    private fun attemptLogin() {
        val loginData = state.loginData ?: return
        mutableStateFlow.update {
            it.copy(
                dialogState = LoginWithDeviceState.DialogState.Loading(
                    message = R.string.logging_in.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.login(
                email = state.emailAddress,
                requestId = loginData.requestId,
                accessCode = loginData.accessCode,
                asymmetricalKey = loginData.asymmetricalKey,
                requestPrivateKey = loginData.privateKey,
                masterPasswordHash = loginData.masterPasswordHash,
                captchaToken = loginData.captchaToken,
            )
            sendAction(LoginWithDeviceAction.Internal.ReceiveLoginResult(result))
        }
    }

    private fun sendNewAuthRequest(isResend: Boolean) {
        setIsResendNotificationLoading(isResend)
        authJob.cancel()
        authJob = authRepository
            .createAuthRequestWithUpdates(email = state.emailAddress)
            .map { LoginWithDeviceAction.Internal.NewAuthRequestResultReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    private fun setIsResendNotificationLoading(isResend: Boolean) {
        updateContent { it.copy(isResendNotificationLoading = isResend) }
    }

    private inline fun updateContent(
        crossinline block: (
            LoginWithDeviceState.ViewState.Content,
        ) -> LoginWithDeviceState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? LoginWithDeviceState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }
}

/**
 * Models state of the Login with Device screen.
 */
@Parcelize
data class LoginWithDeviceState(
    val emailAddress: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
    val loginData: LoginData?,
) : Parcelable {
    /**
     * Represents the specific view states for the [LoginWithDeviceScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Loading state for the [LoginWithDeviceScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Content state for the [LoginWithDeviceScreen] showing the actual content or items.
         *
         * @property fingerprintPhrase The fingerprint phrase to present to the user.
         */
        @Parcelize
        data class Content(
            val fingerprintPhrase: String,
            val isResendNotificationLoading: Boolean,
        ) : ViewState()
    }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Displays an loading dialog to the user.
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Displays an error dialog to the user.
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()
    }

    /**
     * Wrapper class containing all data needed to login.
     */
    @Parcelize
    data class LoginData(
        val accessCode: String,
        val requestId: String,
        val captchaToken: String?,
        val masterPasswordHash: String?,
        val asymmetricalKey: String,
        val privateKey: String,
    ) : Parcelable
}

/**
 * Models events for the Login with Device screen.
 */
sealed class LoginWithDeviceEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : LoginWithDeviceEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val uri: Uri) : LoginWithDeviceEvent()

    /**
     * Navigates to the two-factor login screen.
     */
    data class NavigateToTwoFactorLogin(
        val emailAddress: String,
    ) : LoginWithDeviceEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : LoginWithDeviceEvent()
}

/**
 * Models actions for the Login with Device screen.
 */
sealed class LoginWithDeviceAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : LoginWithDeviceAction()

    /**
     * Indicates that the dialog should be dismissed.
     */
    data object DismissDialog : LoginWithDeviceAction()

    /**
     * Indicates that the "Resend notification" text has been clicked.
     */
    data object ResendNotificationClick : LoginWithDeviceAction()

    /**
     * Indicates that the "View all log in options" text has been clicked.
     */
    data object ViewAllLogInOptionsClick : LoginWithDeviceAction()

    /**
     * Models actions for internal use by the view model.
     */
    sealed class Internal : LoginWithDeviceAction() {
        /**
         * A new auth request result was received.
         */
        data class NewAuthRequestResultReceive(
            val result: CreateAuthRequestResult,
        ) : Internal()

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
