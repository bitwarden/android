package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.prefixHttpsIfNecessary
import com.bitwarden.ui.platform.manager.intent.model.AuthTabData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CookieCallbackResult
import com.x8bit.bitwarden.data.platform.manager.CookieAcquisitionRequestManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkCookieManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val PROXY_URL = "/proxy-cookie-redirect-connector.html"
private const val COOKIE_CALLBACK_URL = "bitwarden://sso-cookie-vendor"
private const val HELP_URL = "https://bitwarden.com/help"

/**
 * ViewModel for the Cookie Acquisition screen.
 */
@HiltViewModel
class CookieAcquisitionViewModel @Inject constructor(
    private val cookieAcquisitionRequestManager: CookieAcquisitionRequestManager,
    private val networkCookieManager: NetworkCookieManager,
    authRepository: AuthRepository,
    environmentRepository: EnvironmentRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CookieAcquisitionState, CookieAcquisitionEvent, CookieAcquisitionAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val environmentUrl = environmentRepository
                .environment
                .environmentUrlData
                .baseWebVaultUrlOrDefault
            val hostname = requireNotNull(
                cookieAcquisitionRequestManager
                    .cookieAcquisitionRequestFlow
                    .value
                    ?.hostname,
            )
            CookieAcquisitionState(
                environmentUrl = environmentUrl,
                hostname = hostname,
                dialogState = null,
            )
        },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        authRepository
            .cookieCallbackResultFlow
            .map { CookieAcquisitionAction.Internal.CookieAcquisitionResultReceived(result = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CookieAcquisitionAction) {
        when (action) {
            CookieAcquisitionAction.LaunchBrowserClick -> handleLaunchBrowserClick()
            CookieAcquisitionAction.ContinueWithoutSyncingClick -> {
                handleContinueWithoutSyncingClick()
            }

            CookieAcquisitionAction.WhyAmISeeingThisClick -> handleWhyAmISeeingThisClick()
            CookieAcquisitionAction.DismissDialogClick -> handleDismissDialogClick()
            is CookieAcquisitionAction.Internal.CookieAcquisitionResultReceived -> {
                handleCookieAcquisitionResultReceived(action)
            }
        }
    }

    private fun handleLaunchBrowserClick() {
        sendEvent(
            CookieAcquisitionEvent.LaunchBrowser(
                uri = "${state.hostname.prefixHttpsIfNecessary()}$PROXY_URL",
                authTabData = AuthTabData.CustomScheme(
                    callbackUrl = COOKIE_CALLBACK_URL,
                ),
            ),
        )
    }

    private fun handleContinueWithoutSyncingClick() {
        cookieAcquisitionRequestManager.setPendingCookieAcquisition(data = null)
        sendEvent(CookieAcquisitionEvent.NavigateBack)
    }

    private fun handleWhyAmISeeingThisClick() {
        sendEvent(CookieAcquisitionEvent.NavigateToHelp(uri = HELP_URL))
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleCookieAcquisitionResultReceived(
        action: CookieAcquisitionAction.Internal.CookieAcquisitionResultReceived,
    ) {
        when (action.result) {
            is CookieCallbackResult.Success -> {
                networkCookieManager.storeCookies(
                    hostname = state.hostname,
                    cookies = action.result.cookies,
                )
                cookieAcquisitionRequestManager.setPendingCookieAcquisition(data = null)
                sendEvent(CookieAcquisitionEvent.NavigateBack)
            }

            is CookieCallbackResult.MissingCookie -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = CookieAcquisitionDialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * State for the Cookie Acquisition screen.
 */
@Parcelize
data class CookieAcquisitionState(
    val environmentUrl: String,
    val hostname: String,
    val dialogState: CookieAcquisitionDialogState?,
) : Parcelable

/**
 * Dialog states for the Cookie Acquisition screen.
 */
sealed class CookieAcquisitionDialogState : Parcelable {
    /**
     * Error dialog when cookie acquisition fails.
     */
    @Parcelize
    data class Error(
        val title: Text,
        val message: Text,
    ) : CookieAcquisitionDialogState()
}

/**
 * Events for the Cookie Acquisition screen.
 */
sealed class CookieAcquisitionEvent {
    /**
     * Launch a browser to acquire cookies.
     */
    data class LaunchBrowser(
        val uri: String,
        val authTabData: AuthTabData,
    ) : CookieAcquisitionEvent()

    /**
     * Navigate to the help page.
     */
    data class NavigateToHelp(val uri: String) : CookieAcquisitionEvent()

    /**
     * Navigate back, dismissing the cookie acquisition screen.
     *
     * Implements [BackgroundEvent] because the cookie callback result may arrive while
     * the screen is not resumed (e.g. returning from a Custom Tab browser session).
     */
    data object NavigateBack : CookieAcquisitionEvent(), BackgroundEvent
}

/**
 * Actions for the Cookie Acquisition screen.
 */
sealed class CookieAcquisitionAction {
    /**
     * User clicked the "Launch browser" button.
     */
    data object LaunchBrowserClick : CookieAcquisitionAction()

    /**
     * User clicked the "Continue without syncing" button.
     */
    data object ContinueWithoutSyncingClick : CookieAcquisitionAction()

    /**
     * User clicked the "Why am I seeing this?" link.
     */
    data object WhyAmISeeingThisClick : CookieAcquisitionAction()

    /**
     * User dismissed the error dialog.
     */
    data object DismissDialogClick : CookieAcquisitionAction()

    /**
     * Internal actions for ViewModel processing.
     */
    sealed class Internal : CookieAcquisitionAction() {
        /**
         * Cookie acquisition result received from the auth repository.
         */
        data class CookieAcquisitionResultReceived(
            val result: CookieCallbackResult,
        ) : Internal()
    }
}
