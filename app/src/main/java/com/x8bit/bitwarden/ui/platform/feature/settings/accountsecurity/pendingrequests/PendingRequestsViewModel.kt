package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestsResult
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
 * View model for the pending login requests screen.
 */
@HiltViewModel
class PendingRequestsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PendingRequestsState, PendingRequestsEvent, PendingRequestsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: PendingRequestsState(
        viewState = PendingRequestsState.ViewState.Loading,
    ),
) {
    private val dateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("M/d/yy hh:mm a")
            .withZone(TimeZone.getDefault().toZoneId())

    init {
        updateAuthRequestList()
    }

    override fun handleAction(action: PendingRequestsAction) {
        when (action) {
            PendingRequestsAction.CloseClick -> handleCloseClicked()
            PendingRequestsAction.DeclineAllRequestsClick -> handleDeclineAllRequestsClicked()
            PendingRequestsAction.LifecycleResume -> handleOnLifecycleResumed()
            is PendingRequestsAction.PendingRequestRowClick -> {
                handlePendingRequestRowClicked(action)
            }

            is PendingRequestsAction.Internal.AuthRequestsResultReceive -> {
                handleAuthRequestsResultReceived(action)
            }
        }
    }

    private fun handleCloseClicked() {
        sendEvent(PendingRequestsEvent.NavigateBack)
    }

    private fun handleDeclineAllRequestsClicked() {
        sendEvent(PendingRequestsEvent.ShowToast("Not yet implemented.".asText()))
    }

    private fun handleOnLifecycleResumed() {
        updateAuthRequestList()
    }

    private fun handlePendingRequestRowClicked(
        action: PendingRequestsAction.PendingRequestRowClick,
    ) {
        sendEvent(PendingRequestsEvent.NavigateToLoginApproval(action.fingerprint))
    }

    private fun handleAuthRequestsResultReceived(
        action: PendingRequestsAction.Internal.AuthRequestsResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                viewState = when (val result = action.authRequestsResult) {
                    is AuthRequestsResult.Success -> {
                        if (result.authRequests.isEmpty()) {
                            PendingRequestsState.ViewState.Empty
                        } else {
                            PendingRequestsState.ViewState.Content(
                                requests = result.authRequests.map { authRequest ->
                                    PendingRequestsState.ViewState.Content.PendingLoginRequest(
                                        fingerprintPhrase = authRequest.fingerprint,
                                        platform = authRequest.platform,
                                        timestamp = dateTimeFormatter.format(
                                            authRequest.creationDate,
                                        ),
                                    )
                                },
                            )
                        }
                    }

                    AuthRequestsResult.Error -> PendingRequestsState.ViewState.Error
                },
            )
        }
    }

    private fun updateAuthRequestList() {
        // TODO BIT-1574: Display pull to refresh
        viewModelScope.launch {
            trySendAction(
                PendingRequestsAction.Internal.AuthRequestsResultReceive(
                    authRequestsResult = authRepository.getAuthRequests(),
                ),
            )
        }
    }
}

/**
 * Models state for the Pending Login Requests screen.
 */
@Parcelize
data class PendingRequestsState(
    val viewState: ViewState,
) : Parcelable {
    /**
     * Represents the specific view states for the [PendingRequestsScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Content state for the [PendingRequestsScreen] listing pending request items.
         */
        @Parcelize
        data class Content(
            val requests: List<PendingLoginRequest>,
        ) : ViewState() {
            /**
             * Models the data for a pending login request.
             */
            @Parcelize
            data class PendingLoginRequest(
                val fingerprintPhrase: String,
                val platform: String,
                val timestamp: String,
            ) : Parcelable
        }

        /**
         * Represents the state wherein there are no pending login requests.
         */
        @Parcelize
        data object Empty : ViewState()

        /**
         * Represents a state where the [PendingRequestsScreen] is unable to display data due to an
         * error retrieving it.
         */
        @Parcelize
        data object Error : ViewState()

        /**
         * Loading state for the [PendingRequestsScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()
    }
}

/**
 * Models events for the delete account screen.
 */
sealed class PendingRequestsEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : PendingRequestsEvent()

    /**
     * Navigates to the Login Approval screen with the given fingerprint.
     */
    data class NavigateToLoginApproval(
        val fingerprint: String,
    ) : PendingRequestsEvent()

    /**
     * Displays the [message] in a toast.
     */
    data class ShowToast(
        val message: Text,
    ) : PendingRequestsEvent()
}

/**
 * Models actions for the delete account screen.
 */
sealed class PendingRequestsAction {

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : PendingRequestsAction()

    /**
     * The user has clicked to deny all login requests.
     */
    data object DeclineAllRequestsClick : PendingRequestsAction()

    /**
     * The screen has been re-opened and should be updated.
     */
    data object LifecycleResume : PendingRequestsAction()

    /**
     * The user has clicked one of the pending request rows.
     */
    data class PendingRequestRowClick(
        val fingerprint: String,
    ) : PendingRequestsAction()

    /**
     * Models actions sent by the view model itself.
     */
    sealed class Internal : PendingRequestsAction() {
        /**
         * Indicates that a new auth requests result has been received.
         */
        data class AuthRequestsResultReceive(
            val authRequestsResult: AuthRequestsResult,
        ) : Internal()
    }
}
