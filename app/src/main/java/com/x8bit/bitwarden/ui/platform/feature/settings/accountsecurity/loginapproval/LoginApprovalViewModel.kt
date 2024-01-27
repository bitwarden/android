package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.loginapproval

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the login approval screen.
 */
@HiltViewModel
class LoginApprovalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginApprovalState, LoginApprovalEvent, LoginApprovalAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginApprovalState(
            fingerprint = LoginApprovalArgs(savedStateHandle).fingerprint,
            viewState = LoginApprovalState.ViewState.Loading,
        ),
) {
    private val dateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("M/d/yy hh:mm a")
            .withZone(TimeZone.getDefault().toZoneId())

    init {
        viewModelScope.launch {
            trySendAction(
                LoginApprovalAction.Internal.AuthRequestResultReceive(
                    authRequestResult = authRepository.getAuthRequest(state.fingerprint),
                ),
            )
        }
    }

    override fun handleAction(action: LoginApprovalAction) {
        when (action) {
            LoginApprovalAction.ApproveRequestClick -> handleApproveRequestClicked()
            LoginApprovalAction.CloseClick -> handleCloseClicked()
            LoginApprovalAction.DeclineRequestClick -> handleDeclineRequestClicked()

            is LoginApprovalAction.Internal.AuthRequestResultReceive -> {
                handleAuthRequestResultReceived(action)
            }
        }
    }

    private fun handleApproveRequestClicked() {
        // TODO BIT-1565 implement approve login request
        sendEvent(LoginApprovalEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleCloseClicked() {
        sendEvent(LoginApprovalEvent.NavigateBack)
    }

    private fun handleDeclineRequestClicked() {
        // TODO BIT-1565 implement decline login request
        sendEvent(LoginApprovalEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleAuthRequestResultReceived(
        action: LoginApprovalAction.Internal.AuthRequestResultReceive,
    ) {
        val email = authRepository.userStateFlow.value?.activeAccount?.email ?: return
        mutableStateFlow.update {
            it.copy(
                viewState = when (val result = action.authRequestResult) {
                    is AuthRequestResult.Success -> {
                        LoginApprovalState.ViewState.Content(
                            deviceType = result.authRequest.platform,
                            domainUrl = result.authRequest.originUrl,
                            email = email,
                            fingerprint = result.authRequest.fingerprint,
                            ipAddress = result.authRequest.ipAddress,
                            time = dateTimeFormatter.format(result.authRequest.creationDate),
                        )
                    }

                    is AuthRequestResult.Error -> LoginApprovalState.ViewState.Error
                },
            )
        }
    }
}

/**
 * Models state for the Login Approval screen.
 */
@Parcelize
data class LoginApprovalState(
    val fingerprint: String,
    val viewState: ViewState,
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
}

/**
 * Models events for the Login Approval screen.
 */
sealed class LoginApprovalEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : LoginApprovalEvent()

    /**
     * Displays the [message] in a toast.
     */
    data class ShowToast(
        val message: Text,
    ) : LoginApprovalEvent()
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
     * Models action the view model could send itself.
     */
    sealed class Internal : LoginApprovalAction() {
        /**
         * An auth request result has been received to populate the data on the screen.
         */
        data class AuthRequestResultReceive(
            val authRequestResult: AuthRequestResult,
        ) : Internal()
    }
}
