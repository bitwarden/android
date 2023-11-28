package com.x8bit.bitwarden.ui.vault.feature.item

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the vault item screen
 */
@HiltViewModel
class VaultItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultItemState, VaultItemEvent, VaultItemAction>(
    initialState = savedStateHandle[KEY_STATE] ?: VaultItemState(
        vaultItemId = VaultItemArgs(savedStateHandle).vaultItemId,
    ),
) {

    init {
        stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultItemAction) {
        when (action) {
            VaultItemAction.CloseClick -> handleCloseClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(VaultItemEvent.NavigateBack)
    }
}

/**
 * Represents the state for viewing an item in the vault.
 */
@Parcelize
data class VaultItemState(
    val vaultItemId: String,
) : Parcelable

/**
 * Represents a set of events related view a vault item.
 */
sealed class VaultItemEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : VaultItemEvent()
}

/**
 * Represents a set of actions related view a vault item.
 */
sealed class VaultItemAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : VaultItemAction()
}
