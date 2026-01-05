package com.x8bit.bitwarden.ui.auth.feature.twofactorlogin

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.network.model.TwoFactorAuthMethod
import com.bitwarden.network.model.TwoFactorDataModel
import com.bitwarden.network.util.availableAuthMethods
import com.bitwarden.network.util.preferredAuthMethod
import com.bitwarden.network.util.twoFactorDisplayEmail
import com.bitwarden.network.util.twoFactorDuoAuthUrl
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.ResendEmailResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForWebAuth
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.button
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.imageRes
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.isContinueButtonEnabled
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.shouldUseNfc
import com.x8bit.bitwarden.ui.auth.feature.twofactorlogin.util.showPasswordInput
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Two-Factor Login screen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class TwoFactorLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    private val resourceManager: ResourceManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TwoFactorLoginState, TwoFactorLoginEvent, TwoFactorLoginAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toTwoFactorLoginArgs()
            TwoFactorLoginState(
                authMethod = authRepository.twoFactorResponse.preferredAuthMethod,
                availableAuthMethods = authRepository.twoFactorResponse.availableAuthMethods,
                codeInput = "",
                displayEmail = authRepository.twoFactorResponse?.twoFactorDisplayEmail
                    ?: args.emailAddress,
                dialogState = null,
                isContinueButtonEnabled = authRepository
                    .twoFactorResponse
                    .preferredAuthMethod
                    .isContinueButtonEnabled,
                isRememberEnabled = false,
                email = args.emailAddress,
                password = args.password,
                orgIdentifier = args.orgIdentifier,
                isNewDeviceVerification = args.isNewDeviceVerification,
            )
        },
) {

    private val recover2faUri: Uri
        get() {
            val baseUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            return "$baseUrl/#/recover-2fa".toUri()
        }

    init {
        // As state updates, write to saved state handle.
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        // Process the Duo result when it is received.
        authRepository
            .duoTokenResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveDuoResult(duoResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        // Fill in the verification code input field when a Yubi Key code is received.
        authRepository
            .yubiKeyResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveYubiKeyResult(yubiKeyResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        // Process the Web Authn result when it is received.
        authRepository
            .webAuthResultFlow
            .map { TwoFactorLoginAction.Internal.ReceiveWebAuthResult(webAuthResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        viewModelScope.launch {
            // If the auth method is email and it is not to verify the device, call resendEmail.
            if (state.authMethod == TwoFactorAuthMethod.EMAIL && !state.isNewDeviceVerification) {
                sendAction(TwoFactorLoginAction.Internal.SendVerificationCodeEmail)
            }
        }
    }

    override fun handleAction(action: TwoFactorLoginAction) {
        when (action) {
            TwoFactorLoginAction.CloseButtonClick -> handleCloseButtonClicked()
            is TwoFactorLoginAction.CodeInputChanged -> handleCodeInputChanged(action)
            TwoFactorLoginAction.ContinueButtonClick -> handleContinueButtonClick()
            TwoFactorLoginAction.DialogDismiss -> handleDialogDismiss()
            is TwoFactorLoginAction.RememberMeToggle -> handleRememberMeToggle(action)
            TwoFactorLoginAction.ResendEmailClick -> handleResendEmailClick()
            is TwoFactorLoginAction.SelectAuthMethod -> handleSelectAuthMethod(action)
            is TwoFactorLoginAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: TwoFactorLoginAction.Internal) {
        when (action) {
            is TwoFactorLoginAction.Internal.ReceiveLoginResult -> handleReceiveLoginResult(action)

            is TwoFactorLoginAction.Internal.ReceiveDuoResult -> {
                handleReceiveDuoResult(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveYubiKeyResult -> {
                handleReceiveYubiKeyResult(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveWebAuthResult -> {
                handleReceiveWebAuthResult(action)
            }

            is TwoFactorLoginAction.Internal.ReceiveResendEmailResult -> {
                handleReceiveResendEmailResult(action)
            }

            TwoFactorLoginAction.Internal.SendVerificationCodeEmail -> {
                handleSendVerificationCodeEmail()
            }
        }
    }

    /**
     * Update the state with the new text and enable or disable the continue button.
     */
    private fun handleCodeInputChanged(action: TwoFactorLoginAction.CodeInputChanged) {
        mutableStateFlow.update {
            it.copy(
                codeInput = action.input,
                isContinueButtonEnabled = action.input.isNotEmpty(),
            )
        }
    }

    /**
     * Navigates to the Duo webpage if appropriate, else processes the login.
     */
    @Suppress("LongMethod")
    private fun handleContinueButtonClick() {
        when (state.authMethod) {
            TwoFactorAuthMethod.DUO,
            TwoFactorAuthMethod.DUO_ORGANIZATION,
                -> {
                val authUrl = authRepository.twoFactorResponse.twoFactorDuoAuthUrl
                // The url should not be empty unless the environment is somehow not supported.
                authUrl
                    ?.let {
                        sendEvent(
                            event = TwoFactorLoginEvent.NavigateToDuo(
                                uri = it.toUri(),
                                scheme = "bitwarden",
                            ),
                        )
                    }
                    ?: mutableStateFlow.update {
                        @Suppress("MaxLineLength")
                        it.copy(
                            dialogState = TwoFactorLoginState.DialogState.Error(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString
                                    .error_connecting_with_the_duo_service_use_a_different_two_step_login_method_or_contact_duo_for_assistance
                                    .asText(),
                            ),
                        )
                    }
            }

            TwoFactorAuthMethod.WEB_AUTH -> {
                sendEvent(
                    event = authRepository
                        .twoFactorResponse
                        ?.authMethodsData
                        ?.get(TwoFactorAuthMethod.WEB_AUTH)
                        ?.let {
                            val uri = generateUriForWebAuth(
                                baseUrl = environmentRepository
                                    .environment
                                    .environmentUrlData
                                    .baseWebVaultUrlOrDefault,
                                data = it,
                                headerText = resourceManager.getString(
                                    resId = BitwardenString.fido2_title,
                                ),
                                buttonText = resourceManager.getString(
                                    resId = BitwardenString.fido2_authenticate_web_authn,
                                ),
                                returnButtonText = resourceManager.getString(
                                    resId = BitwardenString.fido2_return_to_app,
                                ),
                            )
                            TwoFactorLoginEvent.NavigateToWebAuth(uri = uri, scheme = "bitwarden")
                        }
                        ?: TwoFactorLoginEvent.ShowSnackbar(
                            message = BitwardenString
                                .there_was_an_error_starting_web_authn_two_factor_authentication
                                .asText(),
                        ),
                )
            }

            TwoFactorAuthMethod.AUTHENTICATOR_APP,
            TwoFactorAuthMethod.EMAIL,
            TwoFactorAuthMethod.YUBI_KEY,
            TwoFactorAuthMethod.U2F,
            TwoFactorAuthMethod.REMEMBER,
            TwoFactorAuthMethod.RECOVERY_CODE,
                -> initiateLogin()
        }
    }

    /**
     * Dismiss the view.
     */
    private fun handleCloseButtonClicked() {
        sendEvent(TwoFactorLoginEvent.NavigateBack)
    }

    /**
     * Dismiss the dialog.
     */
    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    /**
     * Handle the login result.
     */
    @Suppress("MaxLineLength", "LongMethod")
    private fun handleReceiveLoginResult(action: TwoFactorLoginAction.Internal.ReceiveLoginResult) {
        // Dismiss the loading overlay.
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (val loginResult = action.loginResult) {

            // NO-OP: This error shouldn't be possible at this stage.
            is LoginResult.TwoFactorRequired -> Unit

            // Display any error with the same invalid verification code message.
            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = loginResult.errorMessage?.asText()
                                ?: BitwardenString.invalid_verification_code.asText(),
                            error = loginResult.error,
                        ),
                    )
                }
            }

            is LoginResult.UnofficialServerError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server
                                .asText(),
                        ),
                    )
                }
            }

            is LoginResult.NewDeviceVerification -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = loginResult.errorMessage?.asText()
                                ?: BitwardenString.invalid_verification_code.asText(),
                        ),
                    )
                }
            }

            // NO-OP: Let the auth flow handle navigation after this.
            is LoginResult.Success -> Unit
            LoginResult.CertificateError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    )
                }
            }

            // NO-OP: This result should not be possible here
            is LoginResult.ConfirmKeyConnectorDomain -> Unit
            LoginResult.EncryptionKeyMigrationRequired -> Unit
        }
    }

    /**
     * Handles the Duo callback result.
     */
    private fun handleReceiveDuoResult(
        action: TwoFactorLoginAction.Internal.ReceiveDuoResult,
    ) {
        when (val result = action.duoResult) {
            is DuoCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DuoCallbackTokenResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        codeInput = result.token,
                    )
                }
                initiateLogin()
            }
        }
    }

    /**
     * Handle the Yubi Key result.
     */
    private fun handleReceiveYubiKeyResult(
        action: TwoFactorLoginAction.Internal.ReceiveYubiKeyResult,
    ) {
        mutableStateFlow.update {
            it.copy(
                codeInput = action.yubiKeyResult.token,
                isContinueButtonEnabled = true,
            )
        }
        initiateLogin()
    }

    /**
     * Handle the web auth result.
     */
    private fun handleReceiveWebAuthResult(
        action: TwoFactorLoginAction.Internal.ReceiveWebAuthResult,
    ) {
        when (val result = action.webAuthResult) {
            is WebAuthResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            message = result.message?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is WebAuthResult.Success -> {
                mutableStateFlow.update { it.copy(codeInput = result.token) }
                initiateLogin()
            }
        }
    }

    /**
     * Handle the resend email result.
     */
    private fun handleReceiveResendEmailResult(
        action: TwoFactorLoginAction.Internal.ReceiveResendEmailResult,
    ) {
        // Dismiss the loading overlay.
        mutableStateFlow.update { it.copy(dialogState = null) }

        when (val result = action.resendEmailResult) {
            // Display a dialog for an error result.
            is ResendEmailResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TwoFactorLoginState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result.message?.asText()
                                ?: BitwardenString.verification_email_not_sent.asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            // Display a toast for a successful result.
            ResendEmailResult.Success -> {
                if (action.isUserInitiated) {
                    sendEvent(
                        TwoFactorLoginEvent.ShowSnackbar(
                            message = BitwardenString.verification_email_sent.asText(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Update the state with the new toggle value.
     */
    private fun handleRememberMeToggle(action: TwoFactorLoginAction.RememberMeToggle) {
        mutableStateFlow.update {
            it.copy(
                isRememberEnabled = action.isChecked,
            )
        }
    }

    /**
     * Resend the verification code email.
     */
    private fun handleResendEmailClick() {
        sendVerificationCodeEmail(isUserInitiated = true)
    }

    /**
     * send the verification code email without user interaction.
     */
    private fun handleSendVerificationCodeEmail() {
        sendVerificationCodeEmail(isUserInitiated = false)
    }

    /**
     * Send the verification code email.
     */
    private fun sendVerificationCodeEmail(isUserInitiated: Boolean) {
        // Ensure that the user is in fact verifying with email.
        if (state.authMethod != TwoFactorAuthMethod.EMAIL) {
            return
        }

        // Show the loading overlay.
        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading(
                    message = BitwardenString.submitting.asText(),
                ),
            )
        }

        // Resend the email notification.
        viewModelScope.launch {
            val result = if (!state.isNewDeviceVerification) {
                authRepository.resendVerificationCodeEmail()
            } else {
                authRepository.resendNewDeviceOtp()
            }
            sendAction(
                TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                    resendEmailResult = result,
                    isUserInitiated = isUserInitiated,
                ),
            )
        }
    }

    /**
     * Update the state with the auth method or opens the url for the recovery code.
     */
    private fun handleSelectAuthMethod(action: TwoFactorLoginAction.SelectAuthMethod) {
        when (action.authMethod) {
            TwoFactorAuthMethod.RECOVERY_CODE -> {
                sendEvent(TwoFactorLoginEvent.NavigateToRecoveryCode(recover2faUri))
            }

            TwoFactorAuthMethod.EMAIL -> {
                if (state.authMethod != TwoFactorAuthMethod.EMAIL) {
                    viewModelScope.launch {
                        val result = authRepository.resendVerificationCodeEmail()
                        sendAction(
                            TwoFactorLoginAction.Internal.ReceiveResendEmailResult(
                                resendEmailResult = result,
                                isUserInitiated = false,
                            ),
                        )
                    }
                }
                updateAuthMethodRelatedState(action.authMethod)
            }

            TwoFactorAuthMethod.AUTHENTICATOR_APP,
            TwoFactorAuthMethod.DUO,
            TwoFactorAuthMethod.YUBI_KEY,
            TwoFactorAuthMethod.U2F,
            TwoFactorAuthMethod.REMEMBER,
            TwoFactorAuthMethod.DUO_ORGANIZATION,
            TwoFactorAuthMethod.WEB_AUTH,
                -> {
                updateAuthMethodRelatedState(action.authMethod)
            }
        }
    }

    private fun updateAuthMethodRelatedState(authMethod: TwoFactorAuthMethod) {
        mutableStateFlow.update {
            it.copy(
                authMethod = authMethod,
                isContinueButtonEnabled = authMethod.isContinueButtonEnabled,
            )
        }
    }

    /**
     * Verify the input and attempt to authenticate with the code.
     */
    private fun initiateLogin() {
        mutableStateFlow.update {
            it.copy(
                dialogState = TwoFactorLoginState.DialogState.Loading(
                    message = BitwardenString.logging_in.asText(),
                ),
            )
        }

        // If the user is manually entering a code, remove any white spaces, just in case.
        val code = when (state.authMethod) {
            TwoFactorAuthMethod.AUTHENTICATOR_APP,
            TwoFactorAuthMethod.EMAIL,
                -> state.codeInput.replace(" ", "")

            TwoFactorAuthMethod.DUO,
            TwoFactorAuthMethod.DUO_ORGANIZATION,
            TwoFactorAuthMethod.YUBI_KEY,
            TwoFactorAuthMethod.U2F,
            TwoFactorAuthMethod.REMEMBER,
            TwoFactorAuthMethod.WEB_AUTH,
            TwoFactorAuthMethod.RECOVERY_CODE,
                -> state.codeInput
        }

        viewModelScope.launch {
            val result = if (state.isNewDeviceVerification) {
                authRepository.login(
                    email = state.email,
                    password = state.password,
                    newDeviceOtp = code,
                    orgIdentifier = state.orgIdentifier,
                )
            } else {
                authRepository.login(
                    email = state.email,
                    password = state.password,
                    twoFactorData = TwoFactorDataModel(
                        code = code,
                        method = state.authMethod.value.toString(),
                        remember = state.isRememberEnabled,
                    ),
                    orgIdentifier = state.orgIdentifier,
                )
            }
            sendAction(
                TwoFactorLoginAction.Internal.ReceiveLoginResult(
                    loginResult = result,
                ),
            )
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
    val dialogState: DialogState?,
    val displayEmail: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberEnabled: Boolean,
    val isNewDeviceVerification: Boolean,
    // Internal
    val email: String,
    val password: String?,
    val orgIdentifier: String?,
) : Parcelable {

    /**
     * The text to display for the button given the [authMethod].
     */
    val buttonText: Text get() = authMethod.button

    /**
     * Indicates if the screen should be listening for NFC events from the operating system.
     */
    val shouldListenForNfc: Boolean get() = authMethod.shouldUseNfc

    /**
     * Indicates whether the code input should be displayed.
     */
    val shouldShowCodeInput: Boolean get() = authMethod.showPasswordInput

    /**
     * The image to display for the given the [authMethod].
     */
    @get:DrawableRes
    val imageRes: Int? get() = authMethod.imageRes

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. It no title
         * is specified a default will be provided.
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
 * Models events for the Two-Factor Login screen.
 */
sealed class TwoFactorLoginEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : TwoFactorLoginEvent()

    /**
     * Navigates to the Duo 2-factor authentication screen.
     */
    data class NavigateToDuo(val uri: Uri, val scheme: String) : TwoFactorLoginEvent()

    /**
     * Navigates to the WebAuth authentication screen.
     */
    data class NavigateToWebAuth(val uri: Uri, val scheme: String) : TwoFactorLoginEvent()

    /**
     * Navigates to the recovery code help page.
     *
     * @param uri The recovery uri.
     */
    data class NavigateToRecoveryCode(val uri: Uri) : TwoFactorLoginEvent()

    /**
     * Shows a snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : TwoFactorLoginEvent() {
        constructor(
            message: Text,
            messageHeader: Text? = null,
            actionLabel: Text? = null,
            withDismissAction: Boolean = false,
        ) : this(
            data = BitwardenSnackbarData(
                message = message,
                messageHeader = messageHeader,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            ),
        )
    }
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
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : TwoFactorLoginAction()

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

    /**
     * Models actions that the [TwoFactorLoginViewModel] itself might send.
     */
    sealed class Internal : TwoFactorLoginAction() {
        /**
         * Indicates that a Dup callback token has been received.
         */
        data class ReceiveDuoResult(
            val duoResult: DuoCallbackTokenResult,
        ) : Internal()

        /**
         * Indicates a Yubi Key result has been received.
         */
        data class ReceiveYubiKeyResult(
            val yubiKeyResult: YubiKeyResult,
        ) : Internal()

        /**
         * Indicates a login result has been received.
         */
        data class ReceiveLoginResult(
            val loginResult: LoginResult,
        ) : Internal()

        /**
         * Indicates a resend email result has been received.
         */
        data class ReceiveResendEmailResult(
            val resendEmailResult: ResendEmailResult,
            val isUserInitiated: Boolean,
        ) : Internal()

        /**
         * Indicates a web auth result has been received.
         */
        data class ReceiveWebAuthResult(
            val webAuthResult: WebAuthResult,
        ) : Internal()

        /**
         * Indicates that the verification code email should be sent.
         */
        data object SendVerificationCodeEmail : Internal()
    }
}
