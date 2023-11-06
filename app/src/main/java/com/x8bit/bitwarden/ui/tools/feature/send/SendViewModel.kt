package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the send screen.
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SendState, SendEvent, SendAction>(
    initialState = savedStateHandle[KEY_STATE] ?: SendState.Empty,
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SendAction): Unit = when (action) {
        SendAction.AddSendClick -> handleSendClick()
        SendAction.SearchClick -> handleSearchClick()
    }

    private fun handleSearchClick() {
        // TODO: navigate to send search BIT-594
        sendEvent(SendEvent.ShowToast("Search Not Implemented".asText()))
    }

    private fun handleSendClick() {
        // TODO: navigate to new send UI BIT-479
        sendEvent(SendEvent.ShowToast("New Send Not Implemented".asText()))
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
     * User clicked add a send.
     */
    data object AddSendClick : SendAction()

    /**
     * User clicked search button.
     */
    data object SearchClick : SendAction()
}

/**
 * Models events for the send screen.
 */
sealed class SendEvent {
    /**
     * Show a toast to the user.
     */
    data class ShowToast(val messsage: Text) : SendEvent()
}
