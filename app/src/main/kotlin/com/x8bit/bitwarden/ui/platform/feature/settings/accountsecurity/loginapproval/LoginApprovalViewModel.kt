@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestResult
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.format.FormatStyle
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the login approval screen.
 */
@HiltViewModel
class LoginApprovalViewModel @Inject constructor(
    private val clock: Clock,
    private val authRepository: AuthRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val toastManager: ToastManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginApprovalState, LoginApprovalEvent, LoginApprovalAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val specialCircumstance = specialCircumstanceManager.specialCircumstance
                as? SpecialCircumstance.PasswordlessRequest
            LoginApprovalState(
                specialCircumstance = specialCircumstance,
                fingerprint = specialCircumstance
                    ?.let { "" }
                    ?: requireNotNull(savedStateHandle.toLoginApprovalArgs().fingerprint),
                masterPasswordHash = null,
                publicKey = "",
                requestId = "",
                viewState = LoginApprovalState.ViewState.Loading,
                dialogState = null,
            )
        },
) {
    init {
        state
            .specialCircumstance
            ?.passwordlessRequestData
            ?.let { passwordlessRequestData ->
                if (authRepository.activeUserId != passwordlessRequestData.userId) {
                    mutableStateFlow.update {
                        it.copy(
                            dialogState = LoginApprovalState.DialogState.ChangeAccount(
                                message = BitwardenString
                                    .login_attempt_from_x_do_you_want_to_switch_to_this_account
                                    .asText(
                                        authRepository
                                            .userStateFlow
                                            .value
                                            ?.accounts
                                            ?.find { account ->
                                                account.userId == passwordlessRequestData.userId
                                            }
                                            ?.email
                                            .orEmpty(),
                                    ),
                            ),
                        )
                    }
                } else {
                    authRepository
                        .getAuthRequestByIdFlow(passwordlessRequestData.loginRequestId)
                        .map { LoginApprovalAction.Internal.AuthRequestResultReceive(it) }
                        .onEach(::sendAction)
                        .launchIn(viewModelScope)
                }
            }
            ?: run {
                authRepository
                    .getAuthRequestByFingerprintFlow(state.fingerprint)
                    .map { LoginApprovalAction.Internal.AuthRequestResultReceive(it) }
                    .onEach(::sendAction)
                    .launchIn(viewModelScope)
            }
    }

    override fun handleAction(action: LoginApprovalAction) {
        when (action) {
            LoginApprovalAction.ApproveRequestClick -> handleApproveRequestClicked()
            LoginApprovalAction.CloseClick -> handleCloseClicked()
            LoginApprovalAction.DeclineRequestClick -> handleDeclineRequestClicked()
            LoginApprovalAction.ErrorDialogDismiss -> handleErrorDialogDismissed()
            LoginApprovalAction.ApproveAccountChangeClick -> handleApproveAccountChangeClick()
            LoginApprovalAction.CancelAccountChangeClick -> handleCancelAccountChangeClick()

            is LoginApprovalAction.Internal.ApproveRequestResultReceive -> {
                handleApproveRequestResultReceived(action)
            }

            is LoginApprovalAction.Internal.AuthRequestResultReceive -> {
                handleAuthRequestResultReceived(action)
            }

            is LoginApprovalAction.Internal.DeclineRequestResultReceive -> {
                handleDeclineRequestResultReceived(action)
            }
        }
    }

    private fun handleApproveRequestClicked() {
        viewModelScope.launch {
            trySendAction(
                LoginApprovalAction.Internal.ApproveRequestResultReceive(
                    result = authRepository.updateAuthRequest(
                        requestId = state.requestId,
                        masterPasswordHash = state.masterPasswordHash,
                        publicKey = state.publicKey,
                        isApproved = true,
                    ),
                ),
            )
        }
    }

    private fun handleCloseClicked() {
        sendClosingEvent()
    }

    private fun handleDeclineRequestClicked() {
        viewModelScope.launch {
            trySendAction(
                LoginApprovalAction.Internal.DeclineRequestResultReceive(
                    result = authRepository.updateAuthRequest(
                        requestId = state.requestId,
                        masterPasswordHash = state.masterPasswordHash,
                        publicKey = state.publicKey,
                        isApproved = false,
                    ),
                ),
            )
        }
    }

    private fun handleErrorDialogDismissed() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    private fun handleApproveAccountChangeClick() {
        state.specialCircumstance?.passwordlessRequestData?.let { data ->
            authRepository.switchAccount(userId = data.userId)
            mutableStateFlow.update { it.copy(dialogState = null) }
            authRepository
                .getAuthRequestByIdFlow(data.loginRequestId)
                .map { LoginApprovalAction.Internal.AuthRequestResultReceive(it) }
                .onEach(::sendAction)
                .launchIn(viewModelScope)
        }
    }

    private fun handleCancelAccountChangeClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendClosingEvent()
    }

    private fun handleApproveRequestResultReceived(
        action: LoginApprovalAction.Internal.ApproveRequestResultReceive,
    ) {
        when (val result = action.result) {
            is AuthRequestResult.Success -> {
                sendClosingEvent(messageId = BitwardenString.login_approved)
            }

            is AuthRequestResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginApprovalState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }
        }
    }

    private fun handleAuthRequestResultReceived(
        action: LoginApprovalAction.Internal.AuthRequestResultReceive,
    ) {
        val email = authRepository.userStateFlow.value?.activeAccount?.email ?: return
        when (val result = action.authRequestResult) {
            is AuthRequestUpdatesResult.Update -> mutableStateFlow.update {
                it.copy(
                    fingerprint = result.authRequest.fingerprint,
                    masterPasswordHash = result.authRequest.masterPasswordHash,
                    publicKey = result.authRequest.publicKey,
                    requestId = result.authRequest.id,
                    viewState = LoginApprovalState.ViewState.Content(
                        deviceType = result.authRequest.platform,
                        domainUrl = result.authRequest.originUrl,
                        email = email,
                        fingerprint = result.authRequest.fingerprint,
                        ipAddress = result.authRequest.ipAddress,
                        time = result.authRequest.creationDate.toFormattedDateTimeStyle(
                            dateStyle = FormatStyle.SHORT,
                            timeStyle = FormatStyle.SHORT,
                            clock = clock,
                        ),
                    ),
                )
            }

            is AuthRequestUpdatesResult.Error -> mutableStateFlow.update {
                it.copy(
                    viewState = LoginApprovalState.ViewState.Error,
                )
            }

            AuthRequestUpdatesResult.Approved,
            AuthRequestUpdatesResult.Declined,
            AuthRequestUpdatesResult.Expired,
                -> {
                sendClosingEvent()
            }
        }
    }

    private fun handleDeclineRequestResultReceived(
        action: LoginApprovalAction.Internal.DeclineRequestResultReceive,
    ) {
        when (val result = action.result) {
            is AuthRequestResult.Success -> {
                sendClosingEvent(messageId = BitwardenString.log_in_denied)
            }

            is AuthRequestResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LoginApprovalState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }
        }
    }

    private fun sendClosingEvent(@StringRes messageId: Int? = null) {
        val shouldFinishWhenComplete = state.specialCircumstance?.shouldFinishWhenComplete == true
        messageId?.let {
            if (shouldFinishWhenComplete) {
                // We are about to exit the app, so we need to use a Toast here.
                toastManager.show(messageId = it)
            } else {
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(message = it.asText()),
                    relay = SnackbarRelay.LOGIN_APPROVAL,
                )
            }
        }
        sendEvent(
            event = if (shouldFinishWhenComplete) {
                LoginApprovalEvent.ExitApp
            } else {
                LoginApprovalEvent.NavigateBack
            },
        )
    }
}

/**
 * Models state for the Login Approval screen.
 */
@Parcelize
data class LoginApprovalState(
    val viewState: ViewState,
    val dialogState: DialogState?,
    // Internal
    val specialCircumstance: SpecialCircumstance.PasswordlessRequest?,
    val fingerprint: String,
    val masterPasswordHash: String?,
    val publicKey: String,
    val requestId: String,
) : Parcelable {
    /**
     * Represents the specific view states for the [LoginApprovalScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Content state for the [LoginApprovalScreen].
         */
        @Parcelize
        data class Content(
            val deviceType: String,
            val domainUrl: String,
            val email: String,
            val fingerprint: String,
            val ipAddress: String,
            val time: String,
        ) : ViewState()

        /**
         * Represents a state where the [LoginApprovalScreen] is unable to display data due to an
         * error retrieving it.
         */
        @Parcelize
        data object Error : ViewState()

        /**
         * Loading state for the [LoginApprovalScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()
    }

    /**
     * Represents the various dialogs that can be displayed.
     */
    @Parcelize
    sealed class DialogState : Parcelable {
        /**
         * Requests permission to change active user.
         */
        data class Error(
            val title: Text?,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * Requests permission to change active user.
         */
        data class ChangeAccount(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the Login Approval screen.
 */
sealed class LoginApprovalEvent {
    /**
     * Closes the app.
     */
    data object ExitApp : LoginApprovalEvent()

    /**
     * Navigates back.
     */
    data object NavigateBack : LoginApprovalEvent()
}

/**
 * Models actions for the Login Approval screen.
 */
sealed class LoginApprovalAction {
    /**
     * The user has clicked the Confirm login button.
     */
    data object ApproveRequestClick : LoginApprovalAction()

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : LoginApprovalAction()

    /**
     * The user has clicked the Decline login button.
     */
    data object DeclineRequestClick : LoginApprovalAction()

    /**
     * User dismissed the error dialog.
     */
    data object ErrorDialogDismiss : LoginApprovalAction()

    /**
     * User approved changing the account.
     */
    data object ApproveAccountChangeClick : LoginApprovalAction()

    /**
     * User dismissed the change account dialog.
     */
    data object CancelAccountChangeClick : LoginApprovalAction()

    /**
     * Models action the view model could send itself.
     */
    sealed class Internal : LoginApprovalAction() {
        /**
         * A new result for a request to approve this request has been received.
         */
        data class ApproveRequestResultReceive(
            val result: AuthRequestResult,
        ) : Internal()

        /**
         * An auth request result has been received to populate the data on the screen.
         */
        data class AuthRequestResultReceive(
            val authRequestResult: AuthRequestUpdatesResult,
        ) : Internal()

        /**
         * A new result for a request to decline this request has been received.
         */
        data class DeclineRequestResultReceive(
            val result: AuthRequestResult,
        ) : Internal()
    }
}
