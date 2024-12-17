package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.OrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifiedOrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SSO_URI
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForCaptcha
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForSso
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseIdentityUrl
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.utils.generateRandomString
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_SSO_DATA = "ssoData"
private const val KEY_SSO_CALLBACK_RESULT = "ssoCallbackResult"
private const val KEY_STATE = "state"
private const val RANDOM_STRING_LENGTH = 64

/**
 * Manages application state for the enterprise single sign on screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class EnterpriseSignOnViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val generatorRepository: GeneratorRepository,
    private val networkConnectionManager: NetworkConnectionManager,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<EnterpriseSignOnState, EnterpriseSignOnEvent, EnterpriseSignOnAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
            captchaToken = null,
        ),
) {

    /**
     * Data needed once a response is received from the SSO backend.
     */
    private var ssoResponseData: SsoResponseData?
        get() = savedStateHandle[KEY_SSO_DATA]
        set(value) {
            savedStateHandle[KEY_SSO_DATA] = value
        }

    private var savedSsoCallbackResult: SsoCallbackResult?
        get() = savedStateHandle[KEY_SSO_CALLBACK_RESULT]
        set(value) {
            savedStateHandle[KEY_SSO_CALLBACK_RESULT] = value
        }

    init {
        authRepository
            .ssoCallbackResultFlow
            .onEach {
                sendAction(EnterpriseSignOnAction.Internal.OnSsoCallbackResult(it))
            }
            .launchIn(viewModelScope)

        // Automatically attempt to login again if a captcha token is received.
        authRepository
            .captchaTokenResultFlow
            .onEach {
                sendAction(
                    EnterpriseSignOnAction.Internal.OnReceiveCaptchaToken(it),
                )
            }
            .launchIn(viewModelScope)

        checkOrganizationDomainSsoDetails()
    }

    override fun handleAction(action: EnterpriseSignOnAction) {
        when (action) {
            EnterpriseSignOnAction.CloseButtonClick -> handleCloseButtonClicked()
            EnterpriseSignOnAction.DialogDismiss -> handleDialogDismissed()
            EnterpriseSignOnAction.LogInClick -> handleLogInClicked()
            is EnterpriseSignOnAction.OrgIdentifierInputChange -> {
                handleOrgIdentifierInputChanged(action)
            }

            is EnterpriseSignOnAction.Internal.OnGenerateUriForSsoResult -> {
                handleOnGenerateUriForSsoResult(action)
            }

            EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure -> {
                handleOnSsoPrevalidationFailure()
            }

            is EnterpriseSignOnAction.Internal.OnOrganizationDomainSsoDetailsReceive -> {
                handleOnOrganizationDomainSsoDetailsReceive(action)
            }

            is EnterpriseSignOnAction.Internal.OnSsoCallbackResult -> {
                handleOnSsoCallbackResult(action)
            }

            is EnterpriseSignOnAction.Internal.OnLoginResult -> {
                handleOnLoginResult(action)
            }

            is EnterpriseSignOnAction.Internal.OnReceiveCaptchaToken -> {
                handleOnReceiveCaptchaToken(action)
            }

            is EnterpriseSignOnAction.Internal.OnVerifiedOrganizationDomainSsoDetailsReceive -> {
                handleOnVerifiedOrganizationDomainSsoDetailsReceive(action)
            }
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(EnterpriseSignOnEvent.NavigateBack)
    }

    private fun handleDialogDismissed() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleLogInClicked() {
        prevalidateSso()
    }

    @Suppress("MaxLineLength")
    private fun handleOnLoginResult(action: EnterpriseSignOnAction.Internal.OnLoginResult) {
        when (val loginResult = action.loginResult) {
            is LoginResult.CaptchaRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    event = EnterpriseSignOnEvent.NavigateToCaptcha(
                        uri = generateUriForCaptcha(captchaId = loginResult.captchaId),
                    ),
                )
            }

            is LoginResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
                            message = loginResult.errorMessage?.asText()
                                ?: R.string.login_sso_error.asText(),
                        ),
                    )
                }
            }

            is LoginResult.UnofficialServerError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
                            message = R.string.this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server
                                .asText(),
                        ),
                    )
                }
            }

            is LoginResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                authRepository.rememberedOrgIdentifier = state.orgIdentifierInput
            }

            is LoginResult.TwoFactorRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    EnterpriseSignOnEvent.NavigateToTwoFactorLogin(
                        emailAddress = EnterpriseSignOnArgs(savedStateHandle).emailAddress,
                        orgIdentifier = state.orgIdentifierInput,
                    ),
                )
            }

            LoginResult.CertificateError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.we_couldnt_verify_the_servers_certificate.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleOnGenerateUriForSsoResult(
        action: EnterpriseSignOnAction.Internal.OnGenerateUriForSsoResult,
    ) {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(EnterpriseSignOnEvent.NavigateToSsoLogin(action.uri))
    }

    private fun handleOnSsoPrevalidationFailure() {
        showDefaultError()
    }

    private fun handleOnOrganizationDomainSsoDetailsFailure() {
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
                orgIdentifierInput = authRepository.rememberedOrgIdentifier ?: "",
            )
        }
    }

    private fun handleOnVerifiedOrganizationDomainSsoDetailsReceive(
        action: EnterpriseSignOnAction.Internal.OnVerifiedOrganizationDomainSsoDetailsReceive,
    ) {
        when (val orgDetails = action.verifiedOrgDomainSsoDetailsResult) {
            is VerifiedOrganizationDomainSsoDetailsResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = null,
                        orgIdentifierInput = authRepository.rememberedOrgIdentifier ?: "",
                    )
                }
            }

            is VerifiedOrganizationDomainSsoDetailsResult.Success -> {
                handleOnVerifiedOrganizationDomainSsoDetailsSuccess(orgDetails)
            }
        }
    }

    private fun handleOnVerifiedOrganizationDomainSsoDetailsSuccess(
        orgDetails: VerifiedOrganizationDomainSsoDetailsResult.Success,
    ) {
        if (orgDetails.verifiedOrganizationDomainSsoDetails.isEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = null,
                    orgIdentifierInput = authRepository.rememberedOrgIdentifier ?: "",
                )
            }
            return
        }

        val organizationIdentifier = orgDetails
            .verifiedOrganizationDomainSsoDetails
            .first()
            .organizationIdentifier

        mutableStateFlow.update {
            it.copy(
                orgIdentifierInput = organizationIdentifier,
            )
        }

        // If the email address is associated with a claimed organization we can proceed to the
        // prevalidation step.
        prevalidateSso()
    }

    private fun handleOnOrganizationDomainSsoDetailsReceive(
        action: EnterpriseSignOnAction.Internal.OnOrganizationDomainSsoDetailsReceive,
    ) {
        when (val orgDetails = action.organizationDomainSsoDetailsResult) {
            is OrganizationDomainSsoDetailsResult.Failure -> {
                handleOnOrganizationDomainSsoDetailsFailure()
            }

            is OrganizationDomainSsoDetailsResult.Success -> {
                handleOnOrganizationDomainSsoDetailsSuccess(orgDetails)
            }
        }
    }

    private fun handleOnOrganizationDomainSsoDetailsSuccess(
        orgDetails: OrganizationDomainSsoDetailsResult.Success,
    ) {
        if (!orgDetails.isSsoAvailable || orgDetails.verifiedDate == null) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = null,
                    orgIdentifierInput = authRepository.rememberedOrgIdentifier ?: "",
                )
            }
            return
        }

        if (orgDetails.organizationIdentifier.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        message = R.string.organization_sso_identifier_required.asText(),
                    ),
                    orgIdentifierInput = authRepository.rememberedOrgIdentifier ?: "",
                )
            }
            return
        }

        mutableStateFlow.update { it.copy(orgIdentifierInput = orgDetails.organizationIdentifier) }

        // If the email address is associated with a claimed organization we can proceed to the
        // prevalidation step.
        prevalidateSso()
    }

    private fun handleOrgIdentifierInputChanged(
        action: EnterpriseSignOnAction.OrgIdentifierInputChange,
    ) {
        mutableStateFlow.update { it.copy(orgIdentifierInput = action.input) }
    }

    private fun handleOnSsoCallbackResult(
        action: EnterpriseSignOnAction.Internal.OnSsoCallbackResult,
    ) {
        savedSsoCallbackResult = action.ssoCallbackResult
        attemptLogin()
    }

    private fun handleOnReceiveCaptchaToken(
        action: EnterpriseSignOnAction.Internal.OnReceiveCaptchaToken,
    ) {
        when (val tokenResult = action.tokenResult) {
            CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = EnterpriseSignOnState.DialogState.Error(
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

    private fun prevalidateSso() {
        if (!networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                )
            }
            return
        }

        val organizationIdentifier = state.orgIdentifierInput
        if (organizationIdentifier.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        message = R.string.validation_field_required.asText(
                            R.string.org_identifier.asText(),
                        ),
                    ),
                )
            }
            return
        }

        showLoading()

        viewModelScope.launch {
            when (val prevalidateSso = authRepository.prevalidateSso(organizationIdentifier)) {
                is PrevalidateSsoResult.Failure -> {
                    sendAction(EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure)
                }

                is PrevalidateSsoResult.Success -> {
                    prepareAndLaunchCustomTab(
                        organizationIdentifier = organizationIdentifier,
                        prevalidateSsoResult = prevalidateSso,
                    )
                }
            }
        }
    }

    private fun attemptLogin() {
        val ssoCallbackResult = requireNotNull(savedSsoCallbackResult)
        val ssoData = requireNotNull(ssoResponseData)

        when (ssoCallbackResult) {
            is SsoCallbackResult.MissingCode -> {
                showDefaultError()
            }

            is SsoCallbackResult.Success -> {
                if (ssoCallbackResult.state == ssoData.state) {
                    showLoading()
                    viewModelScope.launch {
                        val result = authRepository
                            .login(
                                email = EnterpriseSignOnArgs(savedStateHandle).emailAddress,
                                ssoCode = ssoCallbackResult.code,
                                ssoCodeVerifier = ssoData.codeVerifier,
                                ssoRedirectUri = SSO_URI,
                                captchaToken = state.captchaToken,
                                organizationIdentifier = state.orgIdentifierInput,
                            )
                        sendAction(EnterpriseSignOnAction.Internal.OnLoginResult(result))
                    }
                } else {
                    showDefaultError()
                }
            }
        }
    }

    private fun checkOrganizationDomainSsoDetails() {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(R.string.loading.asText()),
            )
        }
        viewModelScope.launch {
            if (featureFlagManager.getFeatureFlag(key = FlagKey.VerifiedSsoDomainEndpoint)) {
                val result = authRepository.getVerifiedOrganizationDomainSsoDetails(
                    email = EnterpriseSignOnArgs(savedStateHandle).emailAddress,
                )
                sendAction(
                    EnterpriseSignOnAction.Internal.OnVerifiedOrganizationDomainSsoDetailsReceive(
                        result,
                    ),
                )
            } else {
                val result = authRepository.getOrganizationDomainSsoDetails(
                    email = EnterpriseSignOnArgs(savedStateHandle).emailAddress,
                )
                sendAction(
                    EnterpriseSignOnAction.Internal.OnOrganizationDomainSsoDetailsReceive(result),
                )
            }
        }
    }

    private suspend fun prepareAndLaunchCustomTab(
        organizationIdentifier: String,
        prevalidateSsoResult: PrevalidateSsoResult.Success,
    ) {
        val codeVerifier = generatorRepository.generateRandomString(RANDOM_STRING_LENGTH)

        // Save this for later so that we can validate the SSO callback response
        val generatedSsoState = generatorRepository
            .generateRandomString(RANDOM_STRING_LENGTH)
            .also {
                ssoResponseData = SsoResponseData(
                    codeVerifier = codeVerifier,
                    state = it,
                )
            }

        val uri = generateUriForSso(
            identityBaseUrl = environmentRepository.environment.environmentUrlData.baseIdentityUrl,
            organizationIdentifier = organizationIdentifier,
            token = prevalidateSsoResult.token,
            state = generatedSsoState,
            codeVerifier = codeVerifier,
        )

        // Hide any dialog since we're about to launch a custom tab and could return without getting
        // a result due to user intervention
        sendAction(EnterpriseSignOnAction.Internal.OnGenerateUriForSsoResult(Uri.parse(uri)))
    }

    private fun showDefaultError() {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = R.string.login_sso_error.asText(),
                ),
            )
        }
    }

    private fun showLoading() {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    R.string.logging_in.asText(),
                ),
            )
        }
    }
}

/**
 * Models state of the enterprise sign on screen.
 */
@Parcelize
data class EnterpriseSignOnState(
    val dialogState: DialogState?,
    val orgIdentifierInput: String,
    val captchaToken: String?,
) : Parcelable {
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
 * Models events for the enterprise sign on screen.
 */
sealed class EnterpriseSignOnEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : EnterpriseSignOnEvent()

    /**
     * Navigates to a custom tab for SSO login using [uri].
     */
    data class NavigateToSsoLogin(val uri: Uri) : EnterpriseSignOnEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val uri: Uri) : EnterpriseSignOnEvent()

    /**
     * Navigates to the set master password screen.
     */
    data object NavigateToSetPassword : EnterpriseSignOnEvent()

    /**
     * Navigates to the two-factor login screen.
     */
    data class NavigateToTwoFactorLogin(
        val emailAddress: String,
        val orgIdentifier: String,
    ) : EnterpriseSignOnEvent()
}

/**
 * Models actions for the enterprise sign on screen.
 */
sealed class EnterpriseSignOnAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : EnterpriseSignOnAction()

    /**
     * Indicates that the current dialog has been dismissed.
     */
    data object DialogDismiss : EnterpriseSignOnAction()

    /**
     * Indicates that the Log In button has been clicked.
     */
    data object LogInClick : EnterpriseSignOnAction()

    /**
     * Indicates that the organization identifier input has changed.
     */
    data class OrgIdentifierInputChange(
        val input: String,
    ) : EnterpriseSignOnAction()

    /**
     * Internal actions for the view model.
     */
    sealed class Internal : EnterpriseSignOnAction() {
        /**
         * A [uri] has been generated to request an SSO result.
         */
        data class OnGenerateUriForSsoResult(val uri: Uri) : Internal()

        /**
         * A login result has been received.
         */
        data class OnLoginResult(val loginResult: LoginResult) : Internal()

        /**
         * An SSO callback result has been received.
         */
        data class OnSsoCallbackResult(val ssoCallbackResult: SsoCallbackResult) : Internal()

        /**
         * SSO prevalidation failed.
         */
        data object OnSsoPrevalidationFailure : Internal()

        /**
         * A result was received when requesting an [OrganizationDomainSsoDetailsResult].
         */
        data class OnOrganizationDomainSsoDetailsReceive(
            val organizationDomainSsoDetailsResult: OrganizationDomainSsoDetailsResult,
        ) : Internal()

        /**
         * A captcha callback result has been received
         */
        data class OnReceiveCaptchaToken(val tokenResult: CaptchaCallbackTokenResult) : Internal()

        /**
         * A result was received when requesting an [VerifiedOrganizationDomainSsoDetailsResult].
         */
        data class OnVerifiedOrganizationDomainSsoDetailsReceive(
            val verifiedOrgDomainSsoDetailsResult: VerifiedOrganizationDomainSsoDetailsResult,
        ) : Internal()
    }
}

/**
 * Data needed by the SSO flow to verify and continue the process after receiving a response.
 *
 * @property state A "state" maintained throughout the SSO process to verify that the response from
 * the server is valid and matches what was originally sent in the request.
 * @property codeVerifier A random string used to generate the code challenge for the initial SSO
 * request.
 */
@Parcelize
data class SsoResponseData(
    val state: String,
    val codeVerifier: String,
) : Parcelable
