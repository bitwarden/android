package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.datasource.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemScreen
import dagger.hilt.android.lifecycle.HiltViewModel
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
@Suppress("TooManyFunctions")
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepo: EnvironmentRepository,
    settingsRepo: SettingsRepository,
    private val vaultRepo: VaultRepository,
    policyManager: PolicyManager,
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
    }

    override fun handleAction(action: SendAction): Unit = when (action) {
        SendAction.AboutSendClick -> handleAboutSendClick()
        SendAction.AddSendClick -> handleAddSendClick()
        SendAction.LockClick -> handleLockClick()
        SendAction.RefreshClick -> handleRefreshClick()
        SendAction.SearchClick -> handleSearchClick()
        SendAction.SyncClick -> handleSyncClick()
        is SendAction.CopyClick -> handleCopyClick(action)
        SendAction.FileTypeClick -> handleFileTypeClick()
        is SendAction.SendClick -> handleSendClick(action)
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
    }

    private fun handlePullToRefreshEnableReceive(
        action: SendAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleDeleteSendResultReceive(action: SendAction.Internal.DeleteSendResultReceive) {
        when (action.result) {
            DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SendEvent.ShowToast(R.string.send_deleted.asText()))
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
                            title = R.string.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SendEvent.ShowToast(message = R.string.send_password_removed.asText()))
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
                            message = R.string.generic_error_message.asText(),
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
        sendEvent(SendEvent.NavigateNewSend)
    }

    private fun handleLockClick() {
        vaultRepo.lockVaultForCurrentUser()
    }

    private fun handleRefreshClick() {
        // No need to update the view state, the vault repo will emit a new state during this time.
        vaultRepo.sync(forced = true)
    }

    private fun handleSearchClick() {
        sendEvent(SendEvent.NavigateToSearch)
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(dialogState = SendState.DialogState.Loading(R.string.syncing.asText()))
        }
        vaultRepo.sync(forced = true)
    }

    private fun handleCopyClick(action: SendAction.CopyClick) {
        clipboardManager.setText(text = action.sendItem.shareUrl)
    }

    private fun handleSendClick(action: SendAction.SendClick) {
        sendEvent(SendEvent.NavigateToEditSend(action.sendItem.id))
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
            it.copy(dialogState = SendState.DialogState.Loading(R.string.deleting.asText()))
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
                    message = R.string.removing_send_password.asText(),
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

    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepo.sync(forced = false)
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
                val iconList: List<IconRes>,
                val shareUrl: String,
                val hasPassword: Boolean,
            ) : Parcelable {
                /**
                 * Indicates the type of send this, a text or file.
                 */
                enum class Type(@DrawableRes val iconRes: Int) {
                    FILE(iconRes = R.drawable.ic_file),
                    TEXT(iconRes = R.drawable.ic_file_text),
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
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
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
    }
}

/**
 * Models events for the send screen.
 */
sealed class SendEvent {
    /**
     * Navigate to the new send screen.
     */
    data object NavigateNewSend : SendEvent()

    /**
     * Navigate to the edit send screen.
     */
    data class NavigateToEditSend(val sendId: String) : SendEvent()

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
     * Show a toast to the user.
     */
    data class ShowToast(val message: Text) : SendEvent()
}
