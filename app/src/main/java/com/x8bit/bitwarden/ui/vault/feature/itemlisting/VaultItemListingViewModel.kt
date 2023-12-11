package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toItemListingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages [VaultItemListingState], handles [VaultItemListingsAction],
 * and launches [VaultItemListingEvent] for the [VaultItemListingScreen].
 */
@HiltViewModel
@Suppress("MagicNumber")
class VaultItemListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = VaultItemListingState(
        itemListingType = VaultItemListingArgs(savedStateHandle = savedStateHandle)
            .vaultItemListingType
            .toItemListingType(),
        viewState = VaultItemListingState.ViewState.Loading,
    ),
) {

    init {
        // TODO fetch real listing data in BIT-1057
        viewModelScope.launch {
            delay(2000)
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultItemListingState.ViewState.NoItems,
                )
            }
        }
    }

    override fun handleAction(action: VaultItemListingsAction) {
        when (action) {
            is VaultItemListingsAction.BackClick -> handleBackClick()
            is VaultItemListingsAction.SearchIconClick -> handleSearchIconClick()
            is VaultItemListingsAction.ItemClick -> handleItemClick(action)
            is VaultItemListingsAction.AddVaultItemClick -> handleAddVaultItemClick()
            is VaultItemListingsAction.RefreshClick -> handleRefreshClick()
        }
    }

    //region VaultItemListing Handlers
    private fun handleRefreshClick() {
        // TODO implement refresh in BIT-1057
        sendEvent(
            event = VaultItemListingEvent.ShowToast(
                text = "Not yet implemented".asText(),
            ),
        )
    }

    private fun handleAddVaultItemClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateToAddVaultItem,
        )
    }

    private fun handleItemClick(action: VaultItemListingsAction.ItemClick) {
        sendEvent(
            event = VaultItemListingEvent.NavigateToVaultItem(
                id = action.id,
            ),
        )
    }

    private fun handleBackClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateBack,
        )
    }

    private fun handleSearchIconClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateToVaultSearchScreen,
        )
    }
    //endregion VaultItemListing Handlers
}

/**
 * Models state for the [VaultItemListingScreen].
 */
data class VaultItemListingState(
    val itemListingType: ItemListingType,
    val viewState: ViewState,
) {

    /**
     * Represents the specific view states for the [VaultItemListingScreen].
     */
    sealed class ViewState {

        /**
         * Loading state for the [VaultItemListingScreen],
         * signifying that the content is being processed.
         */
        data object Loading : ViewState()

        /**
         * Represents a state where the [VaultItemListingScreen] has no items to display.
         */
        data object NoItems : ViewState()

        /**
         * Content state for the [VaultItemListingScreen] showing the actual content or items.
         *
         * @property displayItemList List of items to display.
         */
        data class Content(
            val displayItemList: List<DisplayItem>,
        ) : ViewState()

        /**
         * Represents an error state for the [VaultItemListingScreen].
         *
         * @property message Error message to display.
         */
        data class Error(
            val message: Text,
        ) : ViewState()
    }

    /**
     * An item to be displayed.
     *
     * @property id the id of the item.
     * @property title title of the item.
     * @property subtitle subtitle of the item.
     * @property uri uri for the icon to be displayed (nullable).
     * @property iconRes the icon to be displayed.
     */
    data class DisplayItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val uri: String?,
        @DrawableRes
        val iconRes: Int,
    )

    /**
     * Represents different types of item listing.
     */
    sealed class ItemListingType {

        /**
         * The title to display at the top of the screen.
         */
        abstract val titleText: Text

        /**
         * Whether or not the screen has a floating action button (FAB).
         */
        abstract val hasFab: Boolean

        /**
         * A Login item listing.
         */
        data object Login : ItemListingType() {
            override val titleText: Text
                get() = R.string.logins.asText()
            override val hasFab: Boolean
                get() = true
        }

        /**
         * A Card item listing.
         */
        data object Card : ItemListingType() {
            override val titleText: Text
                get() = R.string.cards.asText()
            override val hasFab: Boolean
                get() = true
        }

        /**
         * An Identity item listing.
         */
        data object Identity : ItemListingType() {
            override val titleText: Text
                get() = R.string.identities.asText()
            override val hasFab: Boolean
                get() = true
        }

        /**
         * A Secure Note item listing.
         */
        data object SecureNote : ItemListingType() {
            override val titleText: Text
                get() = R.string.secure_notes.asText()
            override val hasFab: Boolean
                get() = true
        }

        /**
         * A Secure Trash item listing.
         */
        data object Trash : ItemListingType() {
            override val titleText: Text
                get() = R.string.trash.asText()
            override val hasFab: Boolean
                get() = false
        }

        /**
         * A Folder item listing.
         *
         * @property folderId the id of the folder.
         * @property folderName the name of the folder.
         */
        data class Folder(
            val folderId: String?,
            // The folderName will always initially be an empty string
            val folderName: String = "",
        ) : ItemListingType() {
            override val titleText: Text
                get() = folderName.asText()
            override val hasFab: Boolean
                get() = false
        }
    }
}

/**
 * Models events for the [VaultItemListingScreen].
 */
sealed class VaultItemListingEvent {

    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateBack : VaultItemListingEvent()

    /**
     * Navigates to the VaultAddItemScreen.
     */
    data object NavigateToAddVaultItem : VaultItemListingEvent()

    /**
     * Navigates to the VaultItemScreen.
     *
     * @property id the id of the item to navigate to.
     */
    data class NavigateToVaultItem(val id: String) : VaultItemListingEvent()

    /**
     * Navigates to the VaultSearchScreen.
     */
    data object NavigateToVaultSearchScreen : VaultItemListingEvent()

    /**
     * Show a toast with the given message.
     *
     * @property text the text to display.
     */
    data class ShowToast(val text: Text) : VaultItemListingEvent()
}

/**
 * Models actions for the [VaultItemListingScreen].
 */
sealed class VaultItemListingsAction {

    /**
     * Click the refresh button.
     */
    data object RefreshClick : VaultItemListingsAction()

    /**
     * Click the back button.
     */
    data object BackClick : VaultItemListingsAction()

    /**
     * Click the search icon.
     */
    data object SearchIconClick : VaultItemListingsAction()

    /**
     * Click the add item button.
     */
    data object AddVaultItemClick : VaultItemListingsAction()

    /**
     * Click on an item.
     *
     * @property id the id of the item that has been clicked.
     */
    data class ItemClick(val id: String) : VaultItemListingsAction()
}
