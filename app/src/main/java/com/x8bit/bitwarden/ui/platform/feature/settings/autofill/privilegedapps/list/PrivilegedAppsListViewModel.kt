package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedapps.list

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppData
import com.x8bit.bitwarden.data.autofill.fido2.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.platform.manager.BitwardenPackageManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [PrivilegedAppsListScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class PrivilegedAppsViewModel @Inject constructor(
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
                handleUserTrustedAppDeleteClick(action)
            }

            is PrivilegedAppsListAction.UserTrustedAppDeleteConfirmClick -> {
                handleUserTrustedAppDeleteConfirmClick(action.app)
            }

            is PrivilegedAppsListAction.DismissDialogClick -> {
                handleDismissDialogClick()
            }

            is PrivilegedAppsListAction.BackClick -> {
                handleBackClick()
            }

            is PrivilegedAppsListAction.Internal.PrivilegedAppDataStateReceive -> {
                handleTrustedAppDataStateReceive(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(PrivilegedAppsListEvent.NavigateBack)
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleTrustedAppDataStateReceive(
        action: PrivilegedAppsListAction.Internal.PrivilegedAppDataStateReceive,
    ) {
        when (val dataState = action.dataState) {
            is DataState.Loaded -> handleTrustedAppDataStateLoaded(dataState)
            DataState.Loading -> handleTrustedAppDataStateLoading()
            is DataState.Pending -> handleTrustedAppDataStatePending(dataState)
            is DataState.Error -> handleTrustedAppDataStateError()
            // Network connection is not required so we ignore NoNetwork state.
            is DataState.NoNetwork -> Unit
        }
    }

    private fun handleTrustedAppDataStateError() {
        mutableStateFlow.update {
            it.copy(
                dialogState = PrivilegedAppsListState.DialogState.General(
                    message = R.string.generic_error_message.asText(),
                ),
            )
        }
    }

    private fun handleTrustedAppDataStateLoaded(
        dataState: DataState.Loaded<PrivilegedAppData>,
    ) {
        updateViewStateWithData(data = dataState.data, dialogState = null)
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

    private fun handleUserTrustedAppDeleteClick(
        action: PrivilegedAppsListAction.UserTrustedAppDeleteClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState =
                    PrivilegedAppsListState.DialogState.ConfirmDeleteTrustedApp(action.app),
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

        mutableStateFlow.update {
            it.copy(
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
            .getAppNameFromPackageNameOrNull(info.packageName),
    )
}

/**
 * Models the state of the [PrivilegedAppsViewModel].
 */
@Parcelize
data class PrivilegedAppsListState(
    val installedApps: ImmutableList<PrivilegedAppListItem>,
    val notInstalledApps: ImmutableList<PrivilegedAppListItem>,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * Models the different dialog states that the [PrivilegedAppsViewModel] may be in.
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
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events that the [PrivilegedAppsViewModel] may send.
 */
sealed class PrivilegedAppsListEvent {

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : PrivilegedAppsListEvent()
}

/**
 * Models actions that the [PrivilegedAppsViewModel] may receive.
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
     * Models actions that the [PrivilegedAppsViewModel] itself may send.
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
