package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages [VaultState], handles [VaultAction], and launches [VaultEvent] for the [VaultScreen].
 */
@HiltViewModel
class VaultViewModel @Inject constructor() : BaseViewModel<VaultState, VaultEvent, VaultAction>(
    initialState = VaultState.Loading,
) {

    init {
        viewModelScope.launch {
            // TODO will need to load actual vault items BIT-205
            @Suppress("MagicNumber")
            delay(2000)
            mutableStateFlow.update { VaultState.NoItems }
        }
    }

    override fun handleAction(action: VaultAction) {
        when (action) {
            VaultAction.AddItemClick -> handleAddItemClick()
            VaultAction.SearchIconClick -> handleSearchIconClick()
        }
    }

    //region VaultAction Handlers
    private fun handleAddItemClick() {
        sendEvent(VaultEvent.NavigateToAddItemScreen)
    }

    private fun handleSearchIconClick() {
        sendEvent(VaultEvent.NavigateToVaultSearchScreen)
    }
    //endregion VaultAction Handlers
}

/**
 * Models state for the [VaultScreen].
 */
sealed class VaultState {
    /**
     * Loading state for the [VaultScreen].
     */
    data object Loading : VaultState()

    /**
     * No items state for the [VaultScreen].
     */
    data object NoItems : VaultState()

    /**
     * Content state for the [VaultScreen].
     */
    data class Content(val itemList: List<String>) : VaultState()
}

/**
 * Models effects for the [VaultScreen].
 */
sealed class VaultEvent {
    /**
     * Navigate to the Vault Search screen.
     */
    data object NavigateToVaultSearchScreen : VaultEvent()

    /**
     * Navigate to the Add Item screen.
     */
    data object NavigateToAddItemScreen : VaultEvent()
}

/**
 * Models actions for the [VaultScreen].
 */
sealed class VaultAction {
    /**
     * Click the add an item button.
     * This can either be the floating action button or actual add an item button.
     */
    data object AddItemClick : VaultAction()

    /**
     * Click the search icon.
     */
    data object SearchIconClick : VaultAction()
}
