@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices

import android.os.Build
import android.os.Parcelable
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.core.util.isOverFiveMinutesOld
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequestsUpdatesResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeviceInfo
import com.x8bit.bitwarden.data.auth.repository.model.GetDeviceResult
import com.x8bit.bitwarden.data.auth.repository.model.GetDevicesResult
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util.toLastActivityLabel
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util.readableDeviceTypeName
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * View model for the Manage Devices screen.
 */
@Suppress("LongParameterList")
@HiltViewModel
class ManageDevicesViewModel @Inject constructor(
    private val clock: Clock,
    private val authRepository: AuthRepository,
    private val resourceManager: ResourceManager,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    settingsRepository: SettingsRepository,
    buildInfoManager: BuildInfoManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ManageDevicesState, ManageDevicesEvent, ManageDevicesAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ManageDevicesState(
        authRequests = emptyList(),
        devices = emptyList(),
        currentDeviceId = null,
        viewState = ManageDevicesState.ViewState.Loading,
        isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
        isRefreshing = false,
        internalHideBottomSheet = false,
        isFdroid = buildInfoManager.isFdroid,
        devicesLoaded = false,
        authRequestsLoaded = false,
    ),
) {
    private var authJob: Job = Job().apply { complete() }
    private var devicesJob: Job = Job().apply { complete() }

    init {
        updateAuthRequestList()
        fetchAllDevices()
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { ManageDevicesAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.LOGIN_APPROVAL)
            .map { ManageDevicesAction.Internal.SnackbarDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ManageDevicesAction) {
        when (action) {
            ManageDevicesAction.CloseClick -> handleCloseClicked()
            ManageDevicesAction.HideBottomSheet -> handleHideBottomSheet()
            ManageDevicesAction.LifecycleResume -> handleOnLifecycleResumed()
            ManageDevicesAction.RefreshPull -> handleRefreshPull()
            is ManageDevicesAction.PendingRequestRowClick -> {
                handlePendingRequestRowClicked(action)
            }

            is ManageDevicesAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleCloseClicked() {
        sendEvent(ManageDevicesEvent.NavigateBack)
    }

    private fun handleHideBottomSheet() {
        mutableStateFlow.update { it.copy(internalHideBottomSheet = true) }
    }

    private fun handleOnLifecycleResumed() {
        updateAuthRequestList()
        fetchAllDevices()
    }

    private fun handleRefreshPull() {
        mutableStateFlow.update {
            it.copy(
                isRefreshing = true,
                devicesLoaded = false,
                authRequestsLoaded = false,
            )
        }
        updateAuthRequestList()
        fetchAllDevices()
    }

    private fun handlePendingRequestRowClicked(
        action: ManageDevicesAction.PendingRequestRowClick,
    ) {
        sendEvent(ManageDevicesEvent.NavigateToLoginApproval(action.fingerprint))
    }

    private fun handleInternalAction(action: ManageDevicesAction.Internal) {
        when (action) {
            is ManageDevicesAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is ManageDevicesAction.Internal.SnackbarDataReceive -> {
                handleSnackbarDataReceive(action)
            }

            is ManageDevicesAction.Internal.AllDevicesResultReceive -> {
                handleAllDevicesResultReceived(action)
            }

            is ManageDevicesAction.Internal.AuthRequestsResultReceive -> {
                handleAuthRequestsResultReceived(action)
            }
        }
    }

    private fun handlePullToRefreshEnableReceive(
        action: ManageDevicesAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleSnackbarDataReceive(
        action: ManageDevicesAction.Internal.SnackbarDataReceive,
    ) {
        sendEvent(ManageDevicesEvent.ShowSnackbar(action.data))
    }

    private fun updateAuthRequestList() {
        authJob.cancel()
        authJob = authRepository
            .getAuthRequestsWithUpdates()
            .map { ManageDevicesAction.Internal.AuthRequestsResultReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    private fun fetchAllDevices() {
        devicesJob.cancel()
        devicesJob = viewModelScope.launch {
            coroutineScope {
                val devicesDeferred = async { authRepository.getDevices() }
                val currentDeviceDeferred = async { authRepository.getDeviceByIdentifier() }
                sendAction(
                    ManageDevicesAction.Internal.AllDevicesResultReceive(
                        devicesResult = devicesDeferred.await(),
                        currentDeviceResult = currentDeviceDeferred.await(),
                    ),
                )
            }
        }
    }

    private fun handleAuthRequestsResultReceived(
        action: ManageDevicesAction.Internal.AuthRequestsResultReceive,
    ) {
        val filteredRequests = when (val result = action.authRequestsUpdatesResult) {
            is AuthRequestsUpdatesResult.Update ->
                result.authRequests.filterRespondedAndExpired(clock = clock)

            is AuthRequestsUpdatesResult.Error -> emptyList()
        }
        mutableStateFlow.update {
            it.copy(
                authRequests = filteredRequests,
                authRequestsLoaded = true,
                isRefreshing = if (state.devicesLoaded) false else it.isRefreshing,
            )
        }
        if (state.devicesLoaded) {
            updateContentWithCurrentData()
        }
    }

    private fun handleAllDevicesResultReceived(
        action: ManageDevicesAction.Internal.AllDevicesResultReceive,
    ) {
        val devicesResult = action.devicesResult as? GetDevicesResult.Success
            ?: run {
                mutableStateFlow.update {
                    it.copy(viewState = ManageDevicesState.ViewState.Error, isRefreshing = false)
                }
                return
            }
        val currentDeviceResult = action.currentDeviceResult as? GetDeviceResult.Success
            ?: run {
                mutableStateFlow.update {
                    it.copy(viewState = ManageDevicesState.ViewState.Error, isRefreshing = false)
                }
                return
            }

        mutableStateFlow.update {
            it.copy(
                devices = devicesResult.devices,
                currentDeviceId = currentDeviceResult.device.id,
                devicesLoaded = true,
                isRefreshing = if (state.authRequestsLoaded) false else it.isRefreshing,
            )
        }
        if (state.authRequestsLoaded) {
            updateContentWithCurrentData()
        }
    }

    private fun updateContentWithCurrentData() {
        val authRequestMap = state.authRequests.associateBy { it.id }
        val items = state.devices
            .sortedWith(
                compareBy<DeviceInfo> { device ->
                    val matchingRequest = device.pendingAuthRequest?.let { authRequestMap[it.id] }
                    when {
                        device.id == state.currentDeviceId -> 0
                        matchingRequest != null -> 1
                        else -> 2
                    }
                }
                    .thenByDescending { it.lastActivityDate }
                    .thenByDescending { it.creationDate },
            )
            .map { device ->
                val matchingRequest = device.pendingAuthRequest?.let { authRequestMap[it.id] }
                val status = when {
                    device.id == state.currentDeviceId -> DeviceSessionStatus.Current
                    matchingRequest != null -> DeviceSessionStatus.Pending
                    else -> DeviceSessionStatus.None
                }
                ManageDevicesState.ViewState.Content.DeviceItem(
                    id = device.id,
                    name = device.name,
                    typeName = device.type.readableDeviceTypeName,
                    isTrusted = device.isTrusted,
                    firstLoginDate = device.creationDate.toFormattedDateTimeStyle(
                        dateStyle = FormatStyle.MEDIUM,
                        timeStyle = FormatStyle.MEDIUM,
                        clock = clock,
                    ),
                    lastActivityLabel = device.lastActivityDate.toLastActivityLabel(
                        clock = clock,
                    ),
                    status = status,
                    fingerprintPhrase = matchingRequest?.fingerprint,
                )
            }
        mutableStateFlow.update {
            it.copy(viewState = ManageDevicesState.ViewState.Content(items = items))
        }
    }
}

/**
 * Models state for the Manage Devices screen.
 */
@Parcelize
data class ManageDevicesState(
    val authRequests: List<AuthRequest>,
    val devices: List<DeviceInfo>,
    val currentDeviceId: String?,
    val viewState: ViewState,
    private val isPullToRefreshSettingEnabled: Boolean,
    val isRefreshing: Boolean,
    private val internalHideBottomSheet: Boolean,
    private val isFdroid: Boolean,
    val devicesLoaded: Boolean,
    val authRequestsLoaded: Boolean,
) : Parcelable {

    /**
     * Indicates that the bottom sheet should be hidden.
     */
    @get:ChecksSdkIntAtLeast(parameter = Build.VERSION_CODES.TIRAMISU)
    val hideBottomSheet: Boolean
        get() = internalHideBottomSheet &&
            !isFdroid &&
            isBuildVersionAtLeast(Build.VERSION_CODES.TIRAMISU)

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Represents the specific view states for the [ManageDevicesScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Content state for the [ManageDevicesScreen] listing device items.
         */
        @Parcelize
        data class Content(
            val items: List<DeviceItem>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true

            /**
             * Models the data for a registered device, optionally with a pending auth request.
             */
            @Parcelize
            data class DeviceItem(
                val id: String,
                val name: String,
                val typeName: Text,
                val isTrusted: Boolean,
                val firstLoginDate: String,
                val lastActivityLabel: Text?,
                val status: DeviceSessionStatus,
                val fingerprintPhrase: String?,
            ) : Parcelable
        }

        /**
         * Represents a state where the [ManageDevicesScreen] is unable to display data due to an
         * error retrieving it.
         */
        @Parcelize
        data object Error : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Loading state for the [ManageDevicesScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
        }
    }
}

/**
 * Models events for the Manage Devices screen.
 */
sealed class ManageDevicesEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : ManageDevicesEvent()

    /**
     * Navigates to the Login Approval screen with the given request ID.
     */
    data class NavigateToLoginApproval(
        val fingerprint: String,
    ) : ManageDevicesEvent()

    /**
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : ManageDevicesEvent(), BackgroundEvent
}

/**
 * Models actions for the Manage Devices screen.
 */
sealed class ManageDevicesAction {

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : ManageDevicesAction()

    /**
     * The user has dismissed the bottom sheet.
     */
    data object HideBottomSheet : ManageDevicesAction()

    /**
     * The screen has been re-opened and should be updated.
     */
    data object LifecycleResume : ManageDevicesAction()

    /**
     * The user has clicked one of the pending request rows.
     */
    data class PendingRequestRowClick(
        val fingerprint: String,
    ) : ManageDevicesAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : ManageDevicesAction()

    /**
     * Models actions sent by the view model itself.
     */
    sealed class Internal : ManageDevicesAction() {
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
         * Indicates that the combined result of fetching all devices has been received.
         */
        data class AllDevicesResultReceive(
            val devicesResult: GetDevicesResult,
            val currentDeviceResult: GetDeviceResult,
        ) : Internal()

        /**
         * Indicates that an auth requests update has been received.
         */
        data class AuthRequestsResultReceive(
            val authRequestsUpdatesResult: AuthRequestsUpdatesResult,
        ) : Internal()
    }
}

/**
 * Represents the session status of a registered device.
 */
enum class DeviceSessionStatus {
    Current,
    Pending,
    None,
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
