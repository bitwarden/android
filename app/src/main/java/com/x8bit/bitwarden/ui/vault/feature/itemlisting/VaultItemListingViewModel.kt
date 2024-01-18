package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages [VaultItemListingState], handles [VaultItemListingsAction],
 * and launches [VaultItemListingEvent] for the [VaultItemListingScreen].
 */
@HiltViewModel
@Suppress("MagicNumber", "TooManyFunctions")
class VaultItemListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = VaultItemListingState(
        itemListingType = VaultItemListingArgs(savedStateHandle = savedStateHandle)
            .vaultItemListingType
            .toItemListingType(),
        viewState = VaultItemListingState.ViewState.Loading,
        baseWebSendUrl = environmentRepository.environment.environmentUrlData.baseWebSendUrl,
        baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
        isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
        dialogState = null,
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
            is VaultItemListingsAction.LockClick -> handleLockClick()
            is VaultItemListingsAction.SyncClick -> handleSyncClick()
            is VaultItemListingsAction.SearchIconClick -> handleSearchIconClick()
            is VaultItemListingsAction.ItemClick -> handleItemClick(action)
            is VaultItemListingsAction.AddVaultItemClick -> handleAddVaultItemClick()
            is VaultItemListingsAction.RefreshClick -> handleRefreshClick()
            is VaultItemListingsAction.CopySendUrlClick -> handleCopySendUrlClick(action)
            is VaultItemListingsAction.DeleteSendClick -> handleDeleteSendClick(action)
            is VaultItemListingsAction.ShareSendUrlClick -> handleShareSendUrlClick(action)
            is VaultItemListingsAction.RemoveSendPasswordClick -> {
                handleRemoveSendPasswordClick(action)
            }

            is VaultItemListingsAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemListingsAction.Internal.IconLoadingSettingReceive -> {
                handleIconsSettingReceived(action)
            }
        }
    }

    //region VaultItemListing Handlers
    private fun handleRefreshClick() {
        vaultRepository.sync()
    }

    private fun handleCopySendUrlClick(action: VaultItemListingsAction.CopySendUrlClick) {
        clipboardManager.setText(text = action.sendUrl)
    }

    private fun handleDeleteSendClick(action: VaultItemListingsAction.DeleteSendClick) {
        // TODO: Implement deletion (BIT-1411)
        sendEvent(VaultItemListingEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleShareSendUrlClick(action: VaultItemListingsAction.ShareSendUrlClick) {
        sendEvent(VaultItemListingEvent.ShowShareSheet(action.sendUrl))
    }

    private fun handleRemoveSendPasswordClick(
        action: VaultItemListingsAction.RemoveSendPasswordClick,
    ) {
        // TODO: Implement password removal (BIT-1411)
        sendEvent(VaultItemListingEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleAddVaultItemClick() {
        val event = when (state.itemListingType) {
            is VaultItemListingState.ItemListingType.Vault -> {
                VaultItemListingEvent.NavigateToAddVaultItem
            }

            is VaultItemListingState.ItemListingType.Send -> {
                VaultItemListingEvent.NavigateToAddSendItem
            }
        }
        sendEvent(event)
    }

    private fun handleItemClick(action: VaultItemListingsAction.ItemClick) {
        val event = when (state.itemListingType) {
            is VaultItemListingState.ItemListingType.Vault -> {
                VaultItemListingEvent.NavigateToVaultItem(id = action.id)
            }

            is VaultItemListingState.ItemListingType.Send -> {
                VaultItemListingEvent.NavigateToSendItem(id = action.id)
            }
        }
        sendEvent(event)
    }

    private fun handleBackClick() {
        sendEvent(
            event = VaultItemListingEvent.NavigateBack,
        )
    }

    private fun handleLockClick() {
        vaultRepository.lockVaultForCurrentUser()
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.syncing.asText(),
                ),
            )
        }
        vaultRepository.sync()
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
            updateStateWithVaultData(vaultData, clearDialogState = false)
        }
    }
    //endregion VaultItemListing Handlers

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        } else {
            mutableStateFlow.update {
                it.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.generic_error_message.asText(),
                    ),
                    dialogState = null,
                )
            }
        }
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
    }

    private fun vaultLoadingReceive() {
        mutableStateFlow.update { it.copy(viewState = VaultItemListingState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        if (vaultData.data != null) {
            updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        } else {
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = VaultItemListingState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat(R.string.internet_connection_required_message.asText()),
                    ),
                    dialogState = null,
                )
            }
        }
    }

    private fun vaultPendingReceive(vaultData: DataState.Pending<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = false)
    }

    private fun updateStateWithVaultData(vaultData: VaultData, clearDialogState: Boolean) {
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
                viewState = when (val listingType = currentState.itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault -> {
                        vaultData
                            .cipherViewList
                            .filter { cipherView ->
                                cipherView.determineListingPredicate(listingType)
                            }
                            .toViewState(
                                baseIconUrl = state.baseIconUrl,
                                isIconLoadingDisabled = state.isIconLoadingDisabled,
                            )
                    }

                    is VaultItemListingState.ItemListingType.Send -> {
                        vaultData
                            .sendViewList
                            .filter { sendView ->
                                sendView.determineListingPredicate(listingType)
                            }
                            .toViewState(baseWebSendUrl = state.baseWebSendUrl)
                    }
                },
                dialogState = currentState.dialogState.takeUnless { clearDialogState },
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
    val baseWebSendUrl: String,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    val dialogState: DialogState?,
) {

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }

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
     * @property overflowOptions list of options for the item's overflow menu.
     */
    data class DisplayItem(
        val id: String,
        val title: String,
        val subtitle: String?,
        val iconData: IconData,
        val overflowOptions: List<OverflowItem>,
    ) {
        /**
         * Represents a single option to be displayed in an [DisplayItem]s overflow menu.
         *
         * @property title the display title of the option.
         * @property action the action to be sent back to the view model when the option is clicks.
         */
        data class OverflowItem(
            val title: Text,
            val action: VaultItemListingsAction,
        )
    }

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
         * Represents different types of vault item listings.
         */
        sealed class Vault : ItemListingType() {

            /**
             * A Login item listing.
             */
            data object Login : Vault() {
                override val titleText: Text get() = R.string.logins.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Card item listing.
             */
            data object Card : Vault() {
                override val titleText: Text get() = R.string.cards.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * An Identity item listing.
             */
            data object Identity : Vault() {
                override val titleText: Text get() = R.string.identities.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Secure Note item listing.
             */
            data object SecureNote : Vault() {
                override val titleText: Text get() = R.string.secure_notes.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Secure Trash item listing.
             */
            data object Trash : Vault() {
                override val titleText: Text get() = R.string.trash.asText()
                override val hasFab: Boolean get() = false
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
            ) : Vault() {
                override val titleText: Text get() = folderName.asText()
                override val hasFab: Boolean get() = false
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
            ) : Vault() {
                override val titleText: Text get() = collectionName.asText()
                override val hasFab: Boolean get() = false
            }
        }

        /**
         * Represents different types of vault item listings.
         */
        sealed class Send : ItemListingType() {
            /**
             * A Send File item listing.
             */
            data object SendFile : Send() {
                override val titleText: Text get() = R.string.file.asText()
                override val hasFab: Boolean get() = true
            }

            /**
             * A Send Text item listing.
             */
            data object SendText : Send() {
                override val titleText: Text get() = R.string.text.asText()
                override val hasFab: Boolean get() = true
            }
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
     * Navigates to the AddSendItemScreen.
     */
    data object NavigateToAddSendItem : VaultItemListingEvent()

    /**
     * Navigates to the AddSendScreen.
     *
     * @property id the id of the send to navigate to.
     */
    data class NavigateToSendItem(val id: String) : VaultItemListingEvent()

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
     * Show a share sheet with the given content.
     */
    data class ShowShareSheet(val content: String) : VaultItemListingEvent()

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
     * Click the lock button.
     */
    data object LockClick : VaultItemListingsAction()

    /**
     * Click the refresh button.
     */
    data object SyncClick : VaultItemListingsAction()

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
     * Click on the copy send URL overflow option.
     */
    data class CopySendUrlClick(val sendUrl: String) : VaultItemListingsAction()

    /**
     * Click on the share send URL overflow option.
     */
    data class ShareSendUrlClick(val sendUrl: String) : VaultItemListingsAction()

    /**
     * Click on the remove password send overflow option.
     */
    data class RemoveSendPasswordClick(val sendId: String) : VaultItemListingsAction()

    /**
     * Click on the delete send overflow option.
     */
    data class DeleteSendClick(val sendId: String) : VaultItemListingsAction()

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
