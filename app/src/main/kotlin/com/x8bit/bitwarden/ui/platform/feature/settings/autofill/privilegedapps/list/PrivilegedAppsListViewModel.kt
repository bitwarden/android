package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.manager.BitwardenPackageManager
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.credentials.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.credentials.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.credentials.repository.model.PrivilegedAppData
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list.model.PrivilegedAppListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [PrivilegedAppsListScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class PrivilegedAppsListViewModel @Inject constructor(
    private val privilegedAppRepository: PrivilegedAppRepository,
    private val bitwardenPackageManager: BitwardenPackageManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PrivilegedAppsListState, PrivilegedAppsListEvent, PrivilegedAppsListAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: PrivilegedAppsListState(
            installedApps = persistentListOf(),
            notInstalledApps = persistentListOf(),
            dialogState = null,
        ),
) {

    init {
        privilegedAppRepository
            .trustedAppDataStateFlow
            .map { PrivilegedAppsListAction.Internal.PrivilegedAppDataStateReceive(it) }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PrivilegedAppsListAction) {
        when (action) {
            is PrivilegedAppsListAction.UserTrustedAppDeleteClick -> {
                handleUserTrustedAppDeleteClick(action.app)
            }

            is PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick -> {
                handleUserTrustedAppDeleteConfirmClick(action.app)
            }

            is PrivilegedAppsListAction.DismissDialogClick -> {
                handleDismissDialogClick()
            }

            is PrivilegedAppsListAction.BackClick -> handleBackClick()

            is PrivilegedAppsListAction.Internal.PrivilegedAppDataStateReceive -> {
                handleTrustedAppDataStateReceive(action.dataState)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(PrivilegedAppsListEvent.NavigateBack)
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleTrustedAppDataStateReceive(dataState: DataState<PrivilegedAppData>) {
        when (dataState) {
            is DataState.Loaded -> handleTrustedAppDataStateLoaded(dataState)
            DataState.Loading -> handleTrustedAppDataStateLoading()
            is DataState.Pending -> handleTrustedAppDataStatePending(dataState)
            is DataState.Error -> handleTrustedAppDataStateError()
            // Network connection is not required so we ignore NoNetwork state.
            is DataState.NoNetwork -> handleTrustedAppDataStateNoNetwork(dataState)
        }
    }

    private fun handleTrustedAppDataStateNoNetwork(
        dataState: DataState.NoNetwork<PrivilegedAppData>,
    ) {
        updateViewStateWithData(data = dataState.data, dialogState = null)
    }

    private fun handleTrustedAppDataStateError() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PrivilegedAppsListState.DialogState.General(
                    message = BitwardenString.generic_error_message.asText(),
                ),
            )
        }
    }

    private fun handleTrustedAppDataStateLoaded(
        state: DataState.Loaded<PrivilegedAppData>,
    ) {
        updateViewStateWithData(data = state.data, dialogState = null)
    }

    private fun handleTrustedAppDataStateLoading() {
        mutableStateFlow.update {
            it.copy(dialogState = PrivilegedAppsListState.DialogState.Loading)
        }
    }

    private fun handleTrustedAppDataStatePending(
        state: DataState.Pending<PrivilegedAppData>,
    ) {
        updateViewStateWithData(state.data, PrivilegedAppsListState.DialogState.Loading)
    }

    private fun handleUserTrustedAppDeleteClick(app: PrivilegedAppListItem) {
        mutableStateFlow.update {
            it.copy(
                dialogState = PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(app),
            )
        }
    }

    private fun handleUserTrustedAppDeleteConfirmClick(app: PrivilegedAppListItem) {
        mutableStateFlow.update {
            it.copy(
                dialogState = PrivilegedAppsListState.DialogState.Loading,
            )
        }
        viewModelScope.launch {
            privilegedAppRepository
                .removeTrustedPrivilegedApp(
                    packageName = app.packageName,
                    signature = app.signature,
                )
        }
    }

    private fun updateViewStateWithData(
        data: PrivilegedAppData?,
        dialogState: PrivilegedAppsListState.DialogState?,
    ) {
        val notInstalledApps = mutableListOf<PrivilegedAppListItem>()
        val installedApps = mutableListOf<PrivilegedAppListItem>()

        data
            ?.googleTrustedApps
            ?.toPrivilegedAppList(
                trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE,
            )
            ?.sortIntoInstalledAndNotInstalledCollections(
                installedApps = installedApps,
                notInstalledApps = notInstalledApps,
            )
        data
            ?.communityTrustedApps
            ?.toPrivilegedAppList(
                trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY,
            )
            ?.sortIntoInstalledAndNotInstalledCollections(
                installedApps = installedApps,
                notInstalledApps = notInstalledApps,
            )
        data
            ?.userTrustedApps
            ?.toPrivilegedAppList(
                trustAuthority = PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER,
            )
            ?.sortIntoInstalledAndNotInstalledCollections(
                installedApps = installedApps,
                notInstalledApps = notInstalledApps,
            )

        mutableStateFlow.update { state ->
            state.copy(
                installedApps = installedApps
                    .sortedBy { it.appName ?: it.packageName }
                    .toImmutableList(),
                notInstalledApps = notInstalledApps
                    .sortedBy { it.packageName }
                    .toImmutableList(),
                dialogState = dialogState,
            )
        }
    }

    private fun List<PrivilegedAppListItem>.sortIntoInstalledAndNotInstalledCollections(
        installedApps: MutableList<PrivilegedAppListItem>,
        notInstalledApps: MutableList<PrivilegedAppListItem>,
    ) {
        this.forEach {
            if (bitwardenPackageManager.isPackageInstalled(it.packageName)) {
                installedApps.add(it)
            } else {
                notInstalledApps.add(it)
            }
        }
    }

    private fun PrivilegedAppAllowListJson.toPrivilegedAppList(
        trustAuthority: PrivilegedAppListItem.PrivilegedAppTrustAuthority,
    ) = this.apps
        .map { it.toPrivilegedAppListItem(trustAuthority) }

    private fun PrivilegedAppAllowListJson.PrivilegedAppJson.toPrivilegedAppListItem(
        trustAuthority: PrivilegedAppListItem.PrivilegedAppTrustAuthority,
    ) = PrivilegedAppListItem(
        packageName = info.packageName,
        signature = info.signatures
            .first()
            .certFingerprintSha256,
        trustAuthority = trustAuthority,
        appName = bitwardenPackageManager
            .getAppLabelForPackageOrNull(info.packageName),
    )
}

/**
 * Models the state of the [PrivilegedAppsListViewModel].
 */
@Parcelize
data class PrivilegedAppsListState(
    val installedApps: ImmutableList<PrivilegedAppListItem>,
    val notInstalledApps: ImmutableList<PrivilegedAppListItem>,
    val dialogState: DialogState?,
) : Parcelable {

    @IgnoredOnParcel
    val notInstalledUserTrustedApps = notInstalledApps
        .filter {
            it.trustAuthority == PrivilegedAppListItem.PrivilegedAppTrustAuthority.USER
        }

    @IgnoredOnParcel
    val notInstalledCommunityTrustedApps = notInstalledApps
        .filter {
            it.trustAuthority == PrivilegedAppListItem.PrivilegedAppTrustAuthority.COMMUNITY
        }

    @IgnoredOnParcel
    val notInstalledGoogleTrustedApps = notInstalledApps
        .filter {
            it.trustAuthority == PrivilegedAppListItem.PrivilegedAppTrustAuthority.GOOGLE
        }

    /**
     * Models the different dialog states that the [PrivilegedAppsListViewModel] may be in.
     */
    sealed class DialogState : Parcelable {

        /**
         * Show the loading dialog.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * Show the confirm delete trusted app dialog.
         */
        @Parcelize
        data class ConfirmDeleteTrustedApp(
            val app: PrivilegedAppListItem,
        ) : DialogState()

        /**
         * Show a general dialog.
         */
        @Parcelize
        data class General(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events that the [PrivilegedAppsListViewModel] may send.
 */
sealed class PrivilegedAppsListEvent {

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : PrivilegedAppsListEvent()
}

/**
 * Models actions that the [PrivilegedAppsListViewModel] may receive.
 */
sealed class PrivilegedAppsListAction {
    /**
     * Navigate back to the previous screen.
     */
    data object BackClick : PrivilegedAppsListAction()

    /**
     * The user has dismissed the current dialog.
     */
    data object DismissDialogClick : PrivilegedAppsListAction()

    /**
     * The user has selected to delete a trusted app from their local trust store.
     */
    data class UserTrustedAppDeleteClick(
        val app: PrivilegedAppListItem,
    ) : PrivilegedAppsListAction()

    /**
     * The user has confirmed that they want to delete a trusted app from their local trust store.
     */
    data class UserTrustedAppDeleteConfirmClick(
        val app: PrivilegedAppListItem,
    ) : PrivilegedAppsListAction()

    /**
     * Models actions that the [PrivilegedAppsListViewModel] itself may send.
     */
    sealed class Internal : PrivilegedAppsListAction() {
        /**
         * Indicates that the trusted app data state has been received.
         */
        data class PrivilegedAppDataStateReceive(
            val dataState: DataState<PrivilegedAppData>,
        ) : Internal()
    }
}
