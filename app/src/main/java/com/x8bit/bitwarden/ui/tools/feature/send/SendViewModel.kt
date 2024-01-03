package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the send screen.
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepo: VaultRepository,
) : BaseViewModel<SendState, SendEvent, SendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: SendState(
            viewState = SendState.ViewState.Loading,
        ),
) {

    init {
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
        is SendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: SendAction.Internal): Unit = when (action) {
        is SendAction.Internal.SendDataReceive -> handleSendDataReceive(action)
    }

    private fun handleSendDataReceive(action: SendAction.Internal.SendDataReceive) {
        when (val dataState = action.sendDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SendState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(viewState = dataState.data.toViewState())
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = SendState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SendState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(viewState = dataState.data.toViewState())
                }
            }
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
        vaultRepo.sync()
    }

    private fun handleSearchClick() {
        // TODO: navigate to send search BIT-594
        sendEvent(SendEvent.ShowToast("Search Not Implemented".asText()))
    }

    private fun handleSyncClick() {
        // TODO: Add loading dialog state BIT-481
        vaultRepo.sync()
    }
}

/**
 * Models state for the Send screen.
 */
@Parcelize
data class SendState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the send screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Indicates if the FAB should be displayed.
         */
        abstract val shouldDisplayFab: Boolean

        /**
         * Show the empty state.
         */
        @Parcelize
        // TODO: Add actual content BIT-481
        data object Content : ViewState() {
            override val shouldDisplayFab: Boolean get() = true
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data object Empty : ViewState() {
            override val shouldDisplayFab: Boolean get() = true
        }

        /**
         * Represents an error state for the [VaultItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val shouldDisplayFab: Boolean get() = false
        }

        /**
         * Show the loading state.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val shouldDisplayFab: Boolean get() = false
        }
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
     * Models actions that the [SendViewModel] itself will send.
     */
    sealed class Internal : SendAction() {
        /**
         * Indicates that the send data has been received.
         */
        data class SendDataReceive(
            val sendDataState: DataState<SendData>,
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
     * Navigate to the about send screen.
     */
    data object NavigateToAboutSend : SendEvent()

    /**
     * Show a toast to the user.
     */
    data class ShowToast(val message: Text) : SendEvent()
}
