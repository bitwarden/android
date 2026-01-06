package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.repository.util.baseIdentityUrl
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LoginResult
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.model.VerifiedOrganizationDomainSsoDetailsResult
import com.x8bit.bitwarden.data.auth.repository.util.SSO_URI
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForSso
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.data.tools.generator.repository.utils.generateRandomString
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
    private val generatorRepository: GeneratorRepository,
    private val networkConnectionManager: NetworkConnectionManager,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<EnterpriseSignOnState, EnterpriseSignOnEvent, EnterpriseSignOnAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
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

            is EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure -> {
                handleOnSsoPrevalidationFailure(action)
            }

            is EnterpriseSignOnAction.Internal.OnSsoCallbackResult -> {
                handleOnSsoCallbackResult(action)
            }

            is EnterpriseSignOnAction.Internal.OnLoginResult -> {
                handleOnLoginResult(action)
            }

            is EnterpriseSignOnAction.Internal.OnVerifiedOrganizationDomainSsoDetailsReceive -> {
                handleOnVerifiedOrganizationDomainSsoDetailsReceive(action)
            }

            EnterpriseSignOnAction.CancelKeyConnectorDomainClick -> {
                handleCancelKeyConnectorDomainClick()
            }

            EnterpriseSignOnAction.ConfirmKeyConnectorDomainClick -> {
                handleConfirmKeyConnectorDomainClick()
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

    @Suppress("LongMethod")
    private fun handleOnLoginResult(action: EnterpriseSignOnAction.Internal.OnLoginResult) {
        when (val loginResult = action.loginResult) {
            is LoginResult.Error -> {
                showError(
                    message = loginResult.errorMessage?.asText()
                        ?: BitwardenString.login_sso_error.asText(),
                    error = loginResult.error,
                )
            }

            is LoginResult.UnofficialServerError -> {
                @Suppress("MaxLineLength")
                showError(
                    message = BitwardenString
                        .this_is_not_a_recognized_bitwarden_server_you_may_need_to_check_with_your_provider_or_update_your_server
                        .asText(),
                )
            }

            is LoginResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                authRepository.rememberedOrgIdentifier = state.orgIdentifierInput
            }

            is LoginResult.TwoFactorRequired -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    EnterpriseSignOnEvent.NavigateToTwoFactorLogin(
                        emailAddress = savedStateHandle.toEnterpriseSignOnArgs().emailAddress,
                        orgIdentifier = state.orgIdentifierInput,
                    ),
                )
            }

            is LoginResult.EncryptionKeyMigrationRequired -> {
                val vaultUrl =
                    environmentRepository
                        .environment
                        .environmentUrlData
                        .baseWebVaultUrlOrDefault

                showError(
                    message = BitwardenString
                        .this_account_will_soon_be_deleted_log_in_at_x_to_continue_using_bitwarden
                        .asText(vaultUrl.toUriOrNull()?.host ?: vaultUrl),
                )
            }

            LoginResult.CertificateError -> {
                showError(
                    message = BitwardenString.we_couldnt_verify_the_servers_certificate.asText(),
                )
            }

            is LoginResult.NewDeviceVerification -> {
                showError(
                    message = loginResult.errorMessage?.asText()
                        ?: BitwardenString.login_sso_error.asText(),
                )
            }

            is LoginResult.ConfirmKeyConnectorDomain -> {
                showKeyConnectorDomainConfirmation(
                    keyConnectorDomain = loginResult.domain,
                )
            }
        }
    }

    private fun handleOnGenerateUriForSsoResult(
        action: EnterpriseSignOnAction.Internal.OnGenerateUriForSsoResult,
    ) {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(
            EnterpriseSignOnEvent.NavigateToSsoLogin(
                uri = action.uri,
                scheme = action.scheme,
            ),
        )
    }

    private fun handleOnSsoPrevalidationFailure(
        action: EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure,
    ) {
        showError(
            message = action.message?.asText() ?: BitwardenString.login_sso_error.asText(),
            error = action.error,
        )
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

    private fun prevalidateSso() {
        if (!networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        title = BitwardenString.internet_connection_required_title.asText(),
                        message = BitwardenString.internet_connection_required_message.asText(),
                    ),
                )
            }
            return
        }

        val organizationIdentifier = state.orgIdentifierInput
        if (organizationIdentifier.isBlank()) {
            showError(
                message = BitwardenString.validation_field_required.asText(
                    BitwardenString.org_identifier.asText(),
                ),
            )
            return
        }

        showLoading()

        viewModelScope.launch {
            when (val prevalidateSso = authRepository.prevalidateSso(organizationIdentifier)) {
                is PrevalidateSsoResult.Failure -> {
                    sendAction(
                        action = EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure(
                            message = prevalidateSso.message,
                            error = prevalidateSso.error,
                        ),
                    )
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
                showError()
            }

            is SsoCallbackResult.Success -> {
                if (ssoCallbackResult.state == ssoData.state) {
                    showLoading()
                    viewModelScope.launch {
                        val result = authRepository
                            .login(
                                email = savedStateHandle.toEnterpriseSignOnArgs().emailAddress,
                                ssoCode = ssoCallbackResult.code,
                                ssoCodeVerifier = ssoData.codeVerifier,
                                ssoRedirectUri = SSO_URI,
                                organizationIdentifier = state.orgIdentifierInput,
                            )
                        sendAction(EnterpriseSignOnAction.Internal.OnLoginResult(result))
                    }
                } else {
                    showError()
                }
            }
        }
    }

    private fun checkOrganizationDomainSsoDetails() {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    BitwardenString.loading.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.getVerifiedOrganizationDomainSsoDetails(
                email = savedStateHandle.toEnterpriseSignOnArgs().emailAddress,
            )
            sendAction(
                EnterpriseSignOnAction.Internal.OnVerifiedOrganizationDomainSsoDetailsReceive(
                    result,
                ),
            )
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
        sendAction(
            EnterpriseSignOnAction.Internal.OnGenerateUriForSsoResult(
                uri = uri,
                scheme = "bitwarden",
            ),
        )
    }

    private fun showError(
        title: Text = BitwardenString.an_error_has_occurred.asText(),
        message: Text = BitwardenString.login_sso_error.asText(),
        error: Throwable? = null,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    title = title,
                    message = message,
                    error = error,
                ),
            )
        }
    }

    private fun showLoading() {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    BitwardenString.logging_in.asText(),
                ),
            )
        }
    }

    private fun showKeyConnectorDomainConfirmation(keyConnectorDomain: String) {
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.KeyConnectorDomain(
                    keyConnectorDomain = keyConnectorDomain,
                ),
            )
        }
    }

    private fun handleConfirmKeyConnectorDomainClick() {
        showLoading()
        viewModelScope.launch {
            val result = authRepository.continueKeyConnectorLogin()
            sendAction(EnterpriseSignOnAction.Internal.OnLoginResult(result))
        }
    }

    private fun handleCancelKeyConnectorDomainClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        authRepository.cancelKeyConnectorLogin()
    }
}

/**
 * Models state of the enterprise sign on screen.
 */
@Parcelize
data class EnterpriseSignOnState(
    val dialogState: DialogState?,
    val orgIdentifierInput: String,
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
            val title: Text,
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

        /**
         * Represents a dialog indicating that the user needs to confirm the [keyConnectorDomain].
         */
        @Parcelize
        data class KeyConnectorDomain(
            val keyConnectorDomain: String,
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
    data class NavigateToSsoLogin(
        val uri: Uri,
        val scheme: String,
    ) : EnterpriseSignOnEvent()

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
     * Indicates that the confirm button has been clicked
     * on the KeyConnector confirmation dialog.
     */
    data object ConfirmKeyConnectorDomainClick : EnterpriseSignOnAction()

    /**
     * Indicates that the cancel button has been clicked
     * on the KeyConnector confirmation dialog.
     */
    data object CancelKeyConnectorDomainClick : EnterpriseSignOnAction()

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
        data class OnGenerateUriForSsoResult(val uri: Uri, val scheme: String) : Internal()

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
        data class OnSsoPrevalidationFailure(
            val message: String?,
            val error: Throwable?,
        ) : Internal()

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
