package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
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
    initialState = savedStateHandle[KEY_STATE] ?: SendState.Empty,
) {
    override fun handleAction(action: SendAction): Unit = when (action) {
        SendAction.AboutSendClick -> handleAboutSendClick()
        SendAction.AddSendClick -> handleAddSendClick()
        SendAction.LockClick -> handleLockClick()
        SendAction.SearchClick -> handleSearchClick()
        SendAction.SyncClick -> handleSyncClick()
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
sealed class SendState : Parcelable {
    /**
     * Show the empty state.
     */
    @Parcelize
    data object Empty : SendState()
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
     * User clicked search button.
     */
    data object SearchClick : SendAction()

    /**
     * User clicked the sync button.
     */
    data object SyncClick : SendAction()
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
