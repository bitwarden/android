package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedappslist

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppAllowListJson
import com.x8bit.bitwarden.data.autofill.fido2.model.PrivilegedAppData
import com.x8bit.bitwarden.data.autofill.fido2.repository.PrivilegedAppRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.privilegedappslist.model.PrivilegedAppListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private val BITWARDEN_HELP_CENTER_USING_PASSKEYS_URI =
    "https://bitwarden.com/help/storing-passkeys/#using-passkeys-with-bitwarden".toUri()

/**
 * View model for the [PrivilegedAppsListScreen].
 */
@HiltViewModel
class PrivilegedAppsViewModel @Inject constructor(
    private val privilegedAppRepository: PrivilegedAppRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<Fido2TrustState, PrivilegedAppsListEvent, PrivilegedAppsListAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: Fido2TrustState(
            googleTrustedApps = emptyList<PrivilegedAppListItem>()
                .toImmutableList<PrivilegedAppListItem>(),
            communityTrustedApps = emptyList<PrivilegedAppListItem>()
                .toImmutableList<PrivilegedAppListItem>(),
            userTrustedApps = emptyList<PrivilegedAppListItem>()
                .toImmutableList<PrivilegedAppListItem>(),
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
                mutableStateFlow.update { it.copy(dialogState = null) }
            }

            is PrivilegedAppsListAction.LaunchHelpCenterClick -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = Fido2TrustState.DialogState.ConfirmLaunchUri(
                            title = R.string.continue_to_help_center.asText(),
                            message = R.string.learn_more_about_using_passkeys_with_bitwarden
                                .asText(),
                            uri = BITWARDEN_HELP_CENTER_USING_PASSKEYS_URI,
                        ),
                    )
                }
            }

            is PrivilegedAppsListAction.BackClick -> sendEvent(PrivilegedAppsListEvent.NavigateBack)

            is PrivilegedAppsListAction.Internal.PrivilegedAppDataStateReceive -> {
                handleTrustedAppDataStateReceive(action.dataState)
            }
        }
    }

    private fun handleTrustedAppDataStateReceive(dataState: DataState<PrivilegedAppData>) {
        when (dataState) {
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
                dialogState = Fido2TrustState.DialogState.General(
                    message = R.string.generic_error_message.asText(),
                ),
            )
        }
    }

    private fun handleTrustedAppDataStateLoaded(
        loaded: DataState.Loaded<PrivilegedAppData>,
    ) {
        mutableStateFlow.update {
            it.copy(
                googleTrustedApps = loaded.data
                    .googleTrustedApps
                    .toImmutablePrivilegedAppList(),
                communityTrustedApps = loaded.data
                    .communityTrustedApps
                    .toImmutablePrivilegedAppList(),
                userTrustedApps = loaded.data
                    .userTrustedApps
                    .toImmutablePrivilegedAppList(),
                dialogState = null,
            )
        }
    }

    private fun handleTrustedAppDataStateLoading() {
        mutableStateFlow.update { it.copy(dialogState = Fido2TrustState.DialogState.Loading) }
    }

    private fun handleTrustedAppDataStatePending(
        state: DataState.Pending<PrivilegedAppData>,
    ) {
        mutableStateFlow.update {
            it.copy(
                googleTrustedApps = state.data
                    .googleTrustedApps
                    .toImmutablePrivilegedAppList(),
                communityTrustedApps = state.data
                    .communityTrustedApps
                    .toImmutablePrivilegedAppList(),
                userTrustedApps = state.data
                    .userTrustedApps
                    .toImmutablePrivilegedAppList(),
                dialogState = Fido2TrustState.DialogState.Loading,
            )
        }
    }

    private fun handleUserTrustedAppDeleteClick(app: PrivilegedAppListItem) {
        mutableStateFlow.update {
            it.copy(
                dialogState = Fido2TrustState.DialogState.ConfirmDeleteTrustedApp(app),
            )
        }
    }

    private fun handleUserTrustedAppDeleteConfirmClick(app: PrivilegedAppListItem) {
        mutableStateFlow.update {
            it.copy(
                dialogState = Fido2TrustState.DialogState.Loading,
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

    private fun PrivilegedAppAllowListJson.toImmutablePrivilegedAppList() = this.apps
        .map { it.toPrivilegedAppListItem() }
        .toImmutableList()

    private fun PrivilegedAppAllowListJson.PrivilegedAppJson.toPrivilegedAppListItem() =
        PrivilegedAppListItem(
            packageName = info.packageName,
            signature = info.signatures
                .first()
                .certFingerprintSha256,
        )
}

/**
 * Models the state of the [PrivilegedAppsViewModel].
 */
@Parcelize
data class Fido2TrustState(
    val googleTrustedApps: ImmutableList<PrivilegedAppListItem>,
    val communityTrustedApps: ImmutableList<PrivilegedAppListItem>,
    val userTrustedApps: ImmutableList<PrivilegedAppListItem>,
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

        /**
         * Show the confirm launch URI dialog.
         */
        @Parcelize
        data class ConfirmLaunchUri(
            val title: Text,
            val message: Text,
            val uri: Uri,
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

    /**
     * Navigate to the given [uri].
     */
    data class NavigateToUri(val uri: Uri) : PrivilegedAppsListEvent()
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
     * The user has clicked the help center button.
     */
    data object LaunchHelpCenterClick : PrivilegedAppsListAction()

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
