package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.determineListingPredicate
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toItemListingType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.updateWithAdditionalDataIfNecessary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages [VaultItemListingState], handles [VaultItemListingsAction],
 * and launches [VaultItemListingEvent] for the [VaultItemListingScreen].
 */
@HiltViewModel
@Suppress("MagicNumber", "TooManyFunctions")
class VaultItemListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = VaultItemListingState(
        itemListingType = VaultItemListingArgs(savedStateHandle = savedStateHandle)
            .vaultItemListingType
            .toItemListingType(),
        viewState = VaultItemListingState.ViewState.Loading,
        baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
        isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
    ),
) {

    init {
        settingsRepository
            .isIconLoadingDisabledFlow
            .onEach { sendAction(VaultItemListingsAction.Internal.IconLoadingSettingReceive(it)) }
            .launchIn(viewModelScope)

        vaultRepository
            .vaultDataStateFlow
            .onEach { sendAction(VaultItemListingsAction.Internal.VaultDataReceive(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultItemListingsAction) {
        when (action) {
            is VaultItemListingsAction.BackClick -> handleBackClick()
            is VaultItemListingsAction.SearchIconClick -> handleSearchIconClick()
            is VaultItemListingsAction.ItemClick -> handleItemClick(action)
            is VaultItemListingsAction.AddVaultItemClick -> handleAddVaultItemClick()
            is VaultItemListingsAction.RefreshClick -> handleRefreshClick()
            is VaultItemListingsAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemListingsAction.Internal.IconLoadingSettingReceive ->
                handleIconsSettingReceived(action)
        }
    }

    //region VaultItemListing Handlers
    private fun handleRefreshClick() {
        vaultRepository.sync()
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

    private fun handleVaultDataReceive(
        action: VaultItemListingsAction.Internal.VaultDataReceive,
    ) {
        when (val vaultData = action.vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun handleIconsSettingReceived(
        action: VaultItemListingsAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled)
        }

        vaultRepository.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData)
        }
    }
    //endregion VaultItemListing Handlers

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data)
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }
        }
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data)
        } else {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(R.string.internet_connection_required_message.asText()),
                    ),
                )
            }
        }
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data)
    }

    private fun updateStateWithVaultData(vaultData: VaultData) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                itemListingType = currentState
                    .itemListingType
                    .updateWithAdditionalDataIfNecessary(
                        folderList = vaultData
                            .folderViewList,
                        collectionList = vaultData
                            .collectionViewList,
                    ),
                viewState = vaultData
                    .cipherViewList
                    .filter { cipherView ->
                        cipherView.determineListingPredicate(currentState.itemListingType)
                    }
                    .toViewState(
                        baseIconUrl = state.baseIconUrl,
                        isIconLoadingDisabled = state.isIconLoadingDisabled,
                    ),
            )
        }
    }
}

/**
 * Models state for the [VaultItemListingScreen].
 */
data class VaultItemListingState(
    val itemListingType: ItemListingType,
    val viewState: ViewState,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
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
     * @property subtitle subtitle of the item (nullable).
     * @property iconData data for the icon to be displayed (nullable).
     */
    data class DisplayItem(
        val id: String,
        val title: String,
        val subtitle: String?,
        val iconData: IconData,
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

        /**
         * A Collection item listing.
         *
         * @property collectionId the ID of the collection.
         * @property collectionName the name of the collection.
         */
        data class Collection(
            val collectionId: String,
            // The collectionName will always initially be an empty string
            val collectionName: String = "",
        ) : ItemListingType() {
            override val titleText: Text
                get() = collectionName.asText()
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

    /**
     * Models actions that the [VaultItemListingViewModel] itself might send.
     */
    sealed class Internal : VaultItemListingsAction() {

        /**
         * Indicates the icon setting was received.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()
    }
}
