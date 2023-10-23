package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.ui.graphics.Color
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
    // TODO retrieve this from the data layer BIT-205
    initialState = VaultState(
        initials = "BW",
        avatarColor = Color.Blue,
        viewState = VaultState.ViewState.Loading,
    ),
) {

    init {
        viewModelScope.launch {
            // TODO will need to load actual vault items BIT-205
            @Suppress("MagicNumber")
            delay(2000)
            mutableStateFlow.update { currentState ->
                currentState.copy(viewState = VaultState.ViewState.NoItems)
            }
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
 * Represents the overall state for the [VaultScreen].
 *
 * @property avatarColor The color of the avatar in HEX format.
 * @property initials The initials to be displayed on the avatar.
 * @property viewState The specific view state representing loading, no items, or content state.
 */
data class VaultState(
    val avatarColor: Color,
    val initials: String,
    val viewState: ViewState,
) {

    /**
     * Represents the specific view states for the [VaultScreen].
     */
    sealed class ViewState {

        /**
         * Loading state for the [VaultScreen], signifying that the content is being processed.
         */
        data object Loading : ViewState()

        /**
         * Represents a state where the [VaultScreen] has no items to display.
         */
        data object NoItems : ViewState()

        /**
         * Content state for the [VaultScreen] showing the actual content or items.
         *
         * @property itemList The list of items to be displayed in the [VaultScreen].
         */
        data class Content(val itemList: List<String>) : ViewState()
    }
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
