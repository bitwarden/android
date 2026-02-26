package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.repository.util.baseWebSendUrl
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the send screen.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepo: AuthRepository,
    settingsRepo: SettingsRepository,
    policyManager: PolicyManager,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepo: EnvironmentRepository,
    private val vaultRepo: VaultRepository,
    private val networkConnectionManager: NetworkConnectionManager,
) : BaseViewModel<SendState, SendEvent, SendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: SendState(
            viewState = SendState.ViewState.Loading,
            dialogState = null,
            isPullToRefreshSettingEnabled = settingsRepo.getPullToRefreshEnabledFlow().value,
            policyDisablesSend = policyManager
                .getActivePolicies(type = PolicyTypeJson.DISABLE_SEND)
                .any(),
            isRefreshing = false,
            isPremiumUser = authRepo.userStateFlow.value?.activeAccount?.isPremium == true,
        ),
) {

    init {
        settingsRepo
            .getPullToRefreshEnabledFlow()
            .map { SendAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        policyManager
            .getActivePoliciesFlow(type = PolicyTypeJson.DISABLE_SEND)
            .map { SendAction.Internal.PolicyUpdateReceive(it.any()) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        vaultRepo
            .sendDataStateFlow
            .map { SendAction.Internal.SendDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        authRepo
            .userStateFlow
            .map { SendAction.Internal.UserStateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.SEND_DELETED, SnackbarRelay.SEND_UPDATED)
            .map { SendAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SendAction): Unit = when (action) {
        SendAction.AboutSendClick -> handleAboutSendClick()
        SendAction.AddSendClick -> handleAddSendClick()
        is SendAction.AddSendSelected -> handleAddSendSelected(action)
        SendAction.LockClick -> handleLockClick()
        SendAction.RefreshClick -> handleRefreshClick()
        SendAction.SearchClick -> handleSearchClick()
        SendAction.SyncClick -> handleSyncClick()
        is SendAction.CopyClick -> handleCopyClick(action)
        SendAction.FileTypeClick -> handleFileTypeClick()
        is SendAction.SendClick -> handleSendClick(action)
        is SendAction.EditClick -> handleEditSendClick(action)
        is SendAction.ViewClick -> handleViewSendClick(action)
        is SendAction.ShareClick -> handleShareClick(action)
        SendAction.TextTypeClick -> handleTextTypeClick()
        is SendAction.DeleteSendClick -> handleDeleteSendClick(action)
        is SendAction.RemovePasswordClick -> handleRemovePasswordClick(action)
        SendAction.DismissDialog -> handleDismissDialog()
        SendAction.RefreshPull -> handleRefreshPull()
        is SendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: SendAction.Internal): Unit = when (action) {
        is SendAction.Internal.PullToRefreshEnableReceive -> {
            handlePullToRefreshEnableReceive(action)
        }

        is SendAction.Internal.DeleteSendResultReceive -> handleDeleteSendResultReceive(action)
        is SendAction.Internal.RemovePasswordSendResultReceive -> {
            handleRemovePasswordSendResultReceive(action)
        }

        is SendAction.Internal.SendDataReceive -> handleSendDataReceive(action)

        is SendAction.Internal.PolicyUpdateReceive -> handlePolicyUpdateReceive(action)

        SendAction.Internal.InternetConnectionErrorReceived -> {
            handleInternetConnectionErrorReceived()
        }

        is SendAction.Internal.UserStateReceive -> handleUserStateReceive(action)
        is SendAction.Internal.SnackbarDataReceived -> handleSnackbarDataReceived(action)
    }

    private fun handleInternetConnectionErrorReceived() {
        mutableStateFlow.update {
            it.copy(
                isRefreshing = false,
                dialogState = SendState.DialogState.Error(
                    BitwardenString.internet_connection_required_title.asText(),
                    BitwardenString.internet_connection_required_message.asText(),
                ),
            )
        }
    }

    private fun handleUserStateReceive(action: SendAction.Internal.UserStateReceive) {
        mutableStateFlow.update {
            it.copy(isPremiumUser = action.userState?.activeAccount?.isPremium == true)
        }
    }

    private fun handleSnackbarDataReceived(action: SendAction.Internal.SnackbarDataReceived) {
        sendEvent(SendEvent.ShowSnackbar(action.data))
    }

    private fun handlePullToRefreshEnableReceive(
        action: SendAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleDeleteSendResultReceive(action: SendAction.Internal.DeleteSendResultReceive) {
        when (val result = action.result) {
            is DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SendEvent.ShowSnackbar(BitwardenString.send_deleted.asText()))
            }
        }
    }

    private fun handleRemovePasswordSendResultReceive(
        action: SendAction.Internal.RemovePasswordSendResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SendState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    SendEvent.ShowSnackbar(
                        message = BitwardenString.password_removed.asText(),
                    ),
                )
            }
        }
    }

    @Suppress("LongMethod")
    private fun handleSendDataReceive(action: SendAction.Internal.SendDataReceive) {
        when (val dataState = action.sendDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SendState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                        dialogState = null,
                        isRefreshing = false,
                    )
                }
            }

            is DataState.NoNetwork,
            is DataState.Loaded,
                -> {
                val data = dataState
                    .data
                    ?: SendData(sendViewList = emptyList())
                mutableStateFlow.update {
                    it.copy(
                        viewState = data.toViewState(
                            baseWebSendUrl = environmentRepo
                                .environment
                                .environmentUrlData
                                .baseWebSendUrl,
                        ),
                        dialogState = null,
                        isRefreshing = false,
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = SendState.ViewState.Loading)
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState.data.toViewState(
                            baseWebSendUrl = environmentRepo
                                .environment
                                .environmentUrlData
                                .baseWebSendUrl,
                        ),
                    )
                }
            }
        }
    }

    private fun handlePolicyUpdateReceive(action: SendAction.Internal.PolicyUpdateReceive) {
        mutableStateFlow.update {
            it.copy(
                policyDisablesSend = action.policyDisablesSend,
            )
        }
    }

    private fun handleAboutSendClick() {
        sendEvent(SendEvent.NavigateToAboutSend)
    }

    private fun handleAddSendClick() {
        mutableStateFlow.update {
            it.copy(dialogState = SendState.DialogState.SelectSendAddType)
        }
    }

    private fun handleAddSendSelected(action: SendAction.AddSendSelected) {
        if (action.sendType == SendItemType.FILE) {
            if (state.policyDisablesSend) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SendState.DialogState.Error(
                            title = null,
                            message = BitwardenString.send_disabled_warning.asText(),
                        ),
                    )
                }
                return
            }
            if (!state.isPremiumUser) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SendState.DialogState.Error(
                            title = BitwardenString.send.asText(),
                            message = BitwardenString.send_file_premium_required.asText(),
                        ),
                    )
                }
                return
            }
        }
        mutableStateFlow.update { it.copy(dialogState = null) }
        sendEvent(SendEvent.NavigateNewSend(sendType = action.sendType))
    }

    private fun handleLockClick() {
        vaultRepo.lockVaultForCurrentUser(isUserInitiated = true)
    }

    private fun handleRefreshClick() {
        // No need to update the view state, the vault repo will emit a new state during this time.
        vaultRepo.sync(forced = true)
    }

    private fun handleSearchClick() {
        sendEvent(SendEvent.NavigateToSearch)
    }

    private fun handleSyncClick() {
        if (networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SendState.DialogState.Loading(
                        message = BitwardenString.syncing.asText(),
                    ),
                )
            }
            vaultRepo.sync(forced = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialogState = SendState.DialogState.Error(
                        BitwardenString.internet_connection_required_title.asText(),
                        BitwardenString.internet_connection_required_message.asText(),
                    ),
                )
            }
        }
    }

    private fun handleCopyClick(action: SendAction.CopyClick) {
        clipboardManager.setText(
            text = action.sendItem.shareUrl,
            toastDescriptorOverride = BitwardenString.send_link.asText(),
        )
    }

    private fun handleSendClick(action: SendAction.SendClick) {
        sendEvent(
            event = SendEvent.NavigateToViewSend(
                sendId = action.sendItem.id,
                sendType = when (action.sendItem.type) {
                    SendState.ViewState.Content.SendItem.Type.FILE -> SendItemType.FILE
                    SendState.ViewState.Content.SendItem.Type.TEXT -> SendItemType.TEXT
                },
            ),
        )
    }

    private fun handleEditSendClick(action: SendAction.EditClick) {
        sendEvent(
            event = SendEvent.NavigateToEditSend(
                sendId = action.sendItem.id,
                sendType = when (action.sendItem.type) {
                    SendState.ViewState.Content.SendItem.Type.FILE -> SendItemType.FILE
                    SendState.ViewState.Content.SendItem.Type.TEXT -> SendItemType.TEXT
                },
            ),
        )
    }

    private fun handleViewSendClick(action: SendAction.ViewClick) {
        sendEvent(
            event = SendEvent.NavigateToViewSend(
                sendId = action.sendItem.id,
                sendType = when (action.sendItem.type) {
                    SendState.ViewState.Content.SendItem.Type.FILE -> SendItemType.FILE
                    SendState.ViewState.Content.SendItem.Type.TEXT -> SendItemType.TEXT
                },
            ),
        )
    }

    private fun handleShareClick(action: SendAction.ShareClick) {
        sendEvent(SendEvent.ShowShareSheet(action.sendItem.shareUrl))
    }

    private fun handleFileTypeClick() {
        sendEvent(SendEvent.NavigateToFileSends)
    }

    private fun handleTextTypeClick() {
        sendEvent(SendEvent.NavigateToTextSends)
    }

    private fun handleDeleteSendClick(action: SendAction.DeleteSendClick) {
        mutableStateFlow.update {
            it.copy(dialogState = SendState.DialogState.Loading(BitwardenString.deleting.asText()))
        }
        viewModelScope.launch {
            val result = vaultRepo.deleteSend(action.sendItem.id)
            sendAction(SendAction.Internal.DeleteSendResultReceive(result))
        }
    }

    private fun handleRemovePasswordClick(action: SendAction.RemovePasswordClick) {
        mutableStateFlow.update {
            it.copy(
                dialogState = SendState.DialogState.Loading(
                    message = BitwardenString.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepo.removePasswordSend(action.sendItem.id)
            sendAction(SendAction.Internal.RemovePasswordSendResultReceive(result))
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    @Suppress("MagicNumber")
    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(250)
            if (networkConnectionManager.isNetworkConnected) {
                vaultRepo.sync(forced = false)
            } else {
                sendAction(SendAction.Internal.InternetConnectionErrorReceived)
            }
        }
    }
}

/**
 * Models state for the Send screen.
 */
@Parcelize
data class SendState(
    val viewState: ViewState,
    val dialogState: DialogState?,
    private val isPullToRefreshSettingEnabled: Boolean,
    val policyDisablesSend: Boolean,
    val isRefreshing: Boolean,
    val isPremiumUser: Boolean,
) : Parcelable {

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Represents the specific view states for the send screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Indicates if the FAB should be displayed.
         */
        abstract val shouldDisplayFab: Boolean

        /**
         * Show the populated state.
         */
        @Parcelize
        data class Content(
            val textTypeCount: Int,
            val fileTypeCount: Int,
            val sendItems: List<SendItem>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
            override val shouldDisplayFab: Boolean get() = true

            /**
             * Represents the an individual send item to be displayed.
             */
            @Parcelize
            data class SendItem(
                val id: String,
                val name: String,
                val deletionDate: String,
                val type: Type,
                val iconList: ImmutableList<IconData>,
                val shareUrl: String,
                val hasPassword: Boolean,
            ) : Parcelable {
                /**
                 * Indicates the type of send this, a text or file.
                 */
                enum class Type(@field:DrawableRes val iconRes: Int) {
                    FILE(iconRes = BitwardenDrawable.ic_file),
                    TEXT(iconRes = BitwardenDrawable.ic_file_text),
                }
            }
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data object Empty : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
            override val shouldDisplayFab: Boolean get() = true
        }

        /**
         * Represents an error state for the [VaultItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
            override val shouldDisplayFab: Boolean get() = false
        }

        /**
         * Show the loading state.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
            override val shouldDisplayFab: Boolean get() = false
        }
    }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Represents a dialog for selecting a send item type to add.
         */
        @Parcelize
        data object SelectSendAddType : DialogState()
    }
}

/**
 * Models actions for the send screen.
 */
sealed class SendAction {
    /**
     * User clicked the about send button.
     */
    data object AboutSendClick : SendAction()

    /**
     * User clicked add a send.
     */
    data object AddSendClick : SendAction()

    /**
     * User has selected a new kind of send to create.
     */
    data class AddSendSelected(
        val sendType: SendItemType,
    ) : SendAction()

    /**
     * User clicked the lock button.
     */
    data object LockClick : SendAction()

    /**
     * User clicked the refresh button.
     */
    data object RefreshClick : SendAction()

    /**
     * User clicked search button.
     */
    data object SearchClick : SendAction()

    /**
     * User clicked the sync button.
     */
    data object SyncClick : SendAction()

    /**
     * User clicked the file type button.
     */
    data object FileTypeClick : SendAction()

    /**
     * User clicked the text type button.
     */
    data object TextTypeClick : SendAction()

    /**
     * User clicked the item row.
     */
    data class SendClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the edit item row.
     */
    data class EditClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the view item row.
     */
    data class ViewClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the copy item button.
     */
    data class CopyClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the share item button.
     */
    data class ShareClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the delete item button.
     */
    data class DeleteSendClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the remove password item button.
     */
    data class RemovePasswordClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * Dismiss the currently displayed dialog.
     */
    data object DismissDialog : SendAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : SendAction()

    /**
     * Models actions that the [SendViewModel] itself will send.
     */
    sealed class Internal : SendAction() {
        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(val isPullToRefreshEnabled: Boolean) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(val result: DeleteSendResult) : Internal()

        /**
         * Indicates a result for removing the password protection from a send has been received.
         */
        data class RemovePasswordSendResultReceive(
            val result: RemovePasswordSendResult,
        ) : Internal()

        /**
         * Indicates that snackbar data has been received from a relay to be displayed.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates what the current [userState] is.
         */
        data class UserStateReceive(
            val userState: UserState?,
        ) : Internal()

        /**
         * Indicates that the send data has been received.
         */
        data class SendDataReceive(
            val sendDataState: DataState<SendData>,
        ) : Internal()

        /**
         * Indicates that a policy update has been received.
         */
        data class PolicyUpdateReceive(
            val policyDisablesSend: Boolean,
        ) : Internal()

        /**
         * Indicates that the there is not internet connection.
         */
        data object InternetConnectionErrorReceived : Internal()
    }
}

/**
 * Models events for the send screen.
 */
sealed class SendEvent {
    /**
     * Navigate to the new send screen.
     */
    data class NavigateNewSend(
        val sendType: SendItemType,
    ) : SendEvent()

    /**
     * Navigate to the edit send screen.
     */
    data class NavigateToEditSend(
        val sendId: String,
        val sendType: SendItemType,
    ) : SendEvent()

    /**
     * Navigate to the view send screen.
     */
    data class NavigateToViewSend(
        val sendId: String,
        val sendType: SendItemType,
    ) : SendEvent()

    /**
     * Navigate to the about send screen.
     */
    data object NavigateToAboutSend : SendEvent()

    /**
     * Navigate to the send file list screen.
     */
    data object NavigateToFileSends : SendEvent()

    /**
     * Navigate to the send search screen.
     */
    data object NavigateToSearch : SendEvent()

    /**
     * Navigate to the send text screen.
     */
    data object NavigateToTextSends : SendEvent()

    /**
     * Show a share sheet with the given content.
     */
    data class ShowShareSheet(val url: String) : SendEvent()

    /**
     * Show a snackbar to the user.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : SendEvent(), BackgroundEvent {
        constructor(
            message: Text,
            messageHeader: Text? = null,
            actionLabel: Text? = null,
            withDismissAction: Boolean = false,
        ) : this(
            data = BitwardenSnackbarData(
                message = message,
                messageHeader = messageHeader,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            ),
        )
    }
}
