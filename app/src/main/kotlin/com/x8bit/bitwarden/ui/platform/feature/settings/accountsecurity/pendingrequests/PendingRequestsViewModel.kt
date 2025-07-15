package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.pendingrequests

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.core.util.isOverFiveMinutesOld
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * View model for the pending login requests screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class PendingRequestsViewModel @Inject constructor(
    private val clock: Clock,
    private val authRepository: AuthRepository,
    snackbarRelayManager: SnackbarRelayManager,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PendingRequestsState, PendingRequestsEvent, PendingRequestsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: PendingRequestsState(
        authRequests = emptyList(),
        viewState = PendingRequestsState.ViewState.Loading,
        isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
        isRefreshing = false,
        hideBottomSheet = false,
    ),
) {
    private var authJob: Job = Job().apply { complete() }

    init {
        updateAuthRequestList()
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { PendingRequestsAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.LOGIN_APPROVAL)
            .map { PendingRequestsAction.Internal.SnackbarDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PendingRequestsAction) {
        when (action) {
            PendingRequestsAction.CloseClick -> handleCloseClicked()
            PendingRequestsAction.DeclineAllRequestsConfirm -> handleDeclineAllRequestsConfirmed()
            PendingRequestsAction.HideBottomSheet -> handleHideBottomSheet()
            PendingRequestsAction.LifecycleResume -> handleOnLifecycleResumed()
            PendingRequestsAction.RefreshPull -> handleRefreshPull()
            is PendingRequestsAction.PendingRequestRowClick -> {
                handlePendingRequestRowClicked(action)
            }

            is PendingRequestsAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleCloseClicked() {
        sendEvent(PendingRequestsEvent.NavigateBack)
    }

    private fun handleDeclineAllRequestsConfirmed() {
        viewModelScope.launch {
            mutableStateFlow.update {
                it.copy(
                    viewState = PendingRequestsState.ViewState.Loading,
                )
            }
            state.authRequests.forEach { request ->
                authRepository.updateAuthRequest(
                    requestId = request.id,
                    masterPasswordHash = request.masterPasswordHash,
                    publicKey = request.publicKey,
                    isApproved = false,
                )
            }
            updateAuthRequestList()
        }
    }

    private fun handleHideBottomSheet() {
        mutableStateFlow.update { it.copy(hideBottomSheet = true) }
    }

    private fun handleOnLifecycleResumed() {
        updateAuthRequestList()
    }

    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        updateAuthRequestList()
    }

    private fun handlePendingRequestRowClicked(
        action: PendingRequestsAction.PendingRequestRowClick,
    ) {
        sendEvent(PendingRequestsEvent.NavigateToLoginApproval(action.fingerprint))
    }

    private fun handleInternalAction(action: PendingRequestsAction.Internal) {
        when (action) {
            is PendingRequestsAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is PendingRequestsAction.Internal.SnackbarDataReceive -> {
                handleSnackbarDataReceive(action)
            }

            is PendingRequestsAction.Internal.AuthRequestsResultReceive -> {
                handleAuthRequestsResultReceived(action)
            }
        }
    }

    private fun handlePullToRefreshEnableReceive(
        action: PendingRequestsAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleSnackbarDataReceive(
        action: PendingRequestsAction.Internal.SnackbarDataReceive,
    ) {
        sendEvent(PendingRequestsEvent.ShowSnackbar(action.data))
    }

    private fun handleAuthRequestsResultReceived(
        action: PendingRequestsAction.Internal.AuthRequestsResultReceive,
    ) {
        when (val result = action.authRequestsUpdatesResult) {
            is AuthRequestsUpdatesResult.Update -> {
                val requests = result
                    .authRequests
                    .filterRespondedAndExpired(clock = clock)
                    .sortedByDescending { request -> request.creationDate }
                    .map { request ->
                        PendingRequestsState.ViewState.Content.PendingLoginRequest(
                            fingerprintPhrase = request.fingerprint,
                            platform = request.platform,
                            timestamp = request.creationDate.toFormattedDateTimeStyle(
                                dateStyle = FormatStyle.SHORT,
                                timeStyle = FormatStyle.SHORT,
                                clock = clock,
                            ),
                        )
                    }
                if (requests.isEmpty()) {
                    mutableStateFlow.update {
                        it.copy(
                            authRequests = emptyList(),
                            viewState = PendingRequestsState.ViewState.Empty,
                        )
                    }
                } else {
                    mutableStateFlow.update {
                        it.copy(
                            authRequests = result.authRequests,
                            viewState = PendingRequestsState.ViewState.Content(
                                requests = requests,
                            ),
                        )
                    }
                }
            }

            is AuthRequestsUpdatesResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        authRequests = emptyList(),
                        viewState = PendingRequestsState.ViewState.Error,
                    )
                }
            }
        }
        mutableStateFlow.update { it.copy(isRefreshing = false) }
    }

    private fun updateAuthRequestList() {
        authJob.cancel()
        authJob = authRepository
            .getAuthRequestsWithUpdates()
            .map { PendingRequestsAction.Internal.AuthRequestsResultReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }
}

/**
 * Models state for the Pending Login Requests screen.
 */
@Parcelize
data class PendingRequestsState(
    val authRequests: List<AuthRequest>,
    val viewState: ViewState,
    private val isPullToRefreshSettingEnabled: Boolean,
    val isRefreshing: Boolean,
    val hideBottomSheet: Boolean,
) : Parcelable {
    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Represents the specific view states for the [PendingRequestsScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Content state for the [PendingRequestsScreen] listing pending request items.
         */
        @Parcelize
        data class Content(
            val requests: List<PendingLoginRequest>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true

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
        data object Empty : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Represents a state where the [PendingRequestsScreen] is unable to display data due to an
         * error retrieving it.
         */
        @Parcelize
        data object Error : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Loading state for the [PendingRequestsScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
        }
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
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : PendingRequestsEvent(), BackgroundEvent
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
     * The user has confirmed they want to deny all login requests.
     */
    data object DeclineAllRequestsConfirm : PendingRequestsAction()

    /**
     * The user has dismissed the bottom sheet.
     */
    data object HideBottomSheet : PendingRequestsAction()

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
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : PendingRequestsAction()

    /**
     * Models actions sent by the view model itself.
     */
    sealed class Internal : PendingRequestsAction() {
        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(
            val isPullToRefreshEnabled: Boolean,
        ) : Internal()

        /**
         * Indicates that a snackbar data was received.
         */
        data class SnackbarDataReceive(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates that a new auth requests result has been received.
         */
        data class AuthRequestsResultReceive(
            val authRequestsUpdatesResult: AuthRequestsUpdatesResult,
        ) : Internal()
    }
}

/**
 * Filters out [AuthRequest]s that match one of the following criteria:
 * * The request has been approved.
 * * The request has been declined (indicated by it not being approved & having a responseDate).
 * * The request has expired (it is at least 5 minutes old).
 */
private fun List<AuthRequest>.filterRespondedAndExpired(clock: Clock) =
    filterNot { request ->
        request.requestApproved ||
            request.responseDate != null ||
            request.creationDate.isOverFiveMinutesOld(clock)
    }
