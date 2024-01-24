package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PrevalidateSsoResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.generateUriForSso
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
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

private const val KEY_SSO_STATE = "ssoState"
private const val KEY_STATE = "state"
private const val RANDOM_STRING_LENGTH = 64

/**
 * Manages application state for the enterprise single sign on screen.
 */
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
     * A "state" maintained throughout the SSO process to verify that the response from the server
     * is valid and matches what was originally sent in the request.
     */
    private var ssoState: String?
        get() = savedStateHandle[KEY_SSO_STATE]
        set(value) {
            savedStateHandle[KEY_SSO_STATE] = value
        }

    init {
        authRepository
            .ssoCallbackResultFlow
            .onEach {
                handleSsoCallbackResult(it)
            }
            .launchIn(viewModelScope)
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
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(EnterpriseSignOnEvent.NavigateBack)
    }

    private fun handleDialogDismissed() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleLogInClicked() {
        // TODO BIT-816: submit request for single sign on
        sendEvent(EnterpriseSignOnEvent.ShowToast("Not yet implemented."))

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

        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Loading(
                    R.string.logging_in.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val prevalidateSsoResult = authRepository.prevalidateSso(organizationIdentifier)
            when (prevalidateSsoResult) {
                is PrevalidateSsoResult.Failure -> {
                    sendAction(EnterpriseSignOnAction.Internal.OnSsoPrevalidationFailure)
                }

                is PrevalidateSsoResult.Success -> {
                    prepareAndLaunchCustomTab(
                        organizationIdentifier = organizationIdentifier,
                        prevalidateSsoResult = prevalidateSsoResult,
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
        mutableStateFlow.update {
            it.copy(
                dialogState = EnterpriseSignOnState.DialogState.Error(
                    message = R.string.login_sso_error.asText(),
                ),
            )
        }
    }

    private fun handleOrgIdentifierInputChanged(
        action: EnterpriseSignOnAction.OrgIdentifierInputChange,
    ) {
        mutableStateFlow.update { it.copy(orgIdentifierInput = action.input) }
    }

    private fun handleSsoCallbackResult(ssoCallbackResult: SsoCallbackResult) {
        // TODO Handle result as last part of BIT-816
    }

    private suspend fun prepareAndLaunchCustomTab(
        organizationIdentifier: String,
        prevalidateSsoResult: PrevalidateSsoResult.Success,
    ) {
        val codeVerifier = generatorRepository.generateRandomString(RANDOM_STRING_LENGTH)

        // Save this for later so that we can validate the SSO callback response
        val generatedSsoState = generatorRepository
            .generateRandomString(RANDOM_STRING_LENGTH)
            .also { ssoState = it }

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
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
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
         * SSO prevalidation failed.
         */
        data object OnSsoPrevalidationFailure : Internal()
    }
}
