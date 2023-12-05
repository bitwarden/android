package com.x8bit.bitwarden.ui.vault.feature.edit

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
 * ViewModel responsible for handling user interactions in the vault edit item screen
 */
@HiltViewModel
class VaultEditItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultEditItemState, VaultEditItemEvent, VaultEditItemAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: VaultEditItemState(
            vaultItemId = VaultEditItemArgs(savedStateHandle).vaultItemId,
        ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultEditItemAction) {
        when (action) {
            VaultEditItemAction.CloseClick -> handleCloseClick()
            VaultEditItemAction.SaveClick -> handleSaveClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(VaultEditItemEvent.NavigateBack)
    }

    private fun handleSaveClick() {
        // TODO: Persist the data to the vault (BIT-502)
        sendEvent(VaultEditItemEvent.ShowToast("Not yet implemented".asText()))
    }
}

/**
 * Represents the state for editing an item to the vault.
 */
@Parcelize
data class VaultEditItemState(
    val vaultItemId: String,
) : Parcelable

/**
 * Represents a set of events that can be emitted during the process of editing an item in the
 * vault. Each subclass of this sealed class denotes a distinct event that can occur.
 */
sealed class VaultEditItemEvent {
    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: Text,
    ) : VaultEditItemEvent()

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : VaultEditItemEvent()
}

/**
 * Represents a set of actions related to the process of editing an item in the vault.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class VaultEditItemAction {
    /**
     * Represents the action when the save button is clicked.
     */
    data object SaveClick : VaultEditItemAction()

    /**
     * User clicked close.
     */
    data object CloseClick : VaultEditItemAction()
}
