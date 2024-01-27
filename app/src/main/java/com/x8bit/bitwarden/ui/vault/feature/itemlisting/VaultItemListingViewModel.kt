package com.x8bit.bitwarden.ui.vault.feature.itemlisting

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.determineListingPredicate
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toItemListingType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toSearchType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.util.updateWithAdditionalDataIfNecessary
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

/**
 * Manages [VaultItemListingState], handles [VaultItemListingsAction],
 * and launches [VaultItemListingEvent] for the [VaultItemListingScreen].
 */
@HiltViewModel
@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
class VaultItemListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val vaultRepository: VaultRepository,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<VaultItemListingState, VaultItemListingEvent, VaultItemListingsAction>(
    initialState = run {
        val specialCircumstance =
            specialCircumstanceManager.specialCircumstance as? SpecialCircumstance.AutofillSelection
        VaultItemListingState(
            itemListingType = VaultItemListingArgs(savedStateHandle = savedStateHandle)
                .vaultItemListingType
                .toItemListingType(),
            viewState = VaultItemListingState.ViewState.Loading,
            vaultFilterType = vaultRepository.vaultFilterType,
            baseWebSendUrl = environmentRepository.environment.environmentUrlData.baseWebSendUrl,
            baseIconUrl = environmentRepository.environment.environmentUrlData.baseIconUrl,
            isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            isPullToRefreshSettingEnabled = settingsRepository.getPullToRefreshEnabledFlow().value,
            dialogState = null,
            autofillSelectionData = specialCircumstance?.autofillSelectionData,
            shouldFinishOnComplete = specialCircumstance?.shouldFinishWhenComplete ?: false,
        )
    },
) {

    init {
        settingsRepository
            .getPullToRefreshEnabledFlow()
            .map { VaultItemListingsAction.Internal.PullToRefreshEnableReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

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
            is VaultItemListingsAction.DismissDialogClick -> handleDismissDialogClick()
            is VaultItemListingsAction.BackClick -> handleBackClick()
            is VaultItemListingsAction.LockClick -> handleLockClick()
            is VaultItemListingsAction.SyncClick -> handleSyncClick()
            is VaultItemListingsAction.SearchIconClick -> handleSearchIconClick()
            is VaultItemListingsAction.OverflowOptionClick -> handleOverflowOptionClick(action)
            is VaultItemListingsAction.ItemClick -> handleItemClick(action)
            is VaultItemListingsAction.AddVaultItemClick -> handleAddVaultItemClick()
            is VaultItemListingsAction.RefreshClick -> handleRefreshClick()
            is VaultItemListingsAction.RefreshPull -> handleRefreshPull()
            is VaultItemListingsAction.Internal -> handleInternalAction(action)
        }
    }

    //region VaultItemListing Handlers
    private fun handleRefreshClick() {
        vaultRepository.sync()
    }

    private fun handleRefreshPull() {
        // The Pull-To-Refresh composable is already in the refreshing state.
        // We will reset that state when sendDataStateFlow emits later on.
        vaultRepository.sync()
    }

    private fun handleCopySendUrlClick(action: ListingItemOverflowAction.SendAction.CopyUrlClick) {
        clipboardManager.setText(text = action.sendUrl)
    }

    private fun handleDeleteSendClick(action: ListingItemOverflowAction.SendAction.DeleteClick) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.deleting.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.deleteSend(action.sendId)
            sendAction(VaultItemListingsAction.Internal.DeleteSendResultReceive(result))
        }
    }

    private fun handleShareSendUrlClick(
        action: ListingItemOverflowAction.SendAction.ShareUrlClick,
    ) {
        sendEvent(VaultItemListingEvent.ShowShareSheet(action.sendUrl))
    }

    private fun handleRemoveSendPasswordClick(
        action: ListingItemOverflowAction.SendAction.RemovePasswordClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = VaultItemListingState.DialogState.Loading(
                    message = R.string.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepository.removePasswordSend(action.sendId)
            sendAction(VaultItemListingsAction.Internal.RemovePasswordSendResultReceive(result))
        }
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

    private fun handleEditSendClick(action: ListingItemOverflowAction.SendAction.EditClick) {
        sendEvent(VaultItemListingEvent.NavigateToSendItem(id = action.sendId))
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

    private fun handleCopyNoteClick(action: ListingItemOverflowAction.VaultAction.CopyNoteClick) {
        clipboardManager.setText(action.notes)
    }

    private fun handleCopyNumberClick(
        action: ListingItemOverflowAction.VaultAction.CopyNumberClick,
    ) {
        clipboardManager.setText(action.number)
    }

    private fun handleCopyPasswordClick(
        action: ListingItemOverflowAction.VaultAction.CopyPasswordClick,
    ) {
        clipboardManager.setText(action.password)
    }

    private fun handleCopySecurityCodeClick(
        action: ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
    ) {
        clipboardManager.setText(action.securityCode)
    }

    private fun handleCopyUsernameClick(
        action: ListingItemOverflowAction.VaultAction.CopyUsernameClick,
    ) {
        clipboardManager.setText(action.username)
    }

    private fun handleEditCipherClick(action: ListingItemOverflowAction.VaultAction.EditClick) {
        sendEvent(VaultItemListingEvent.NavigateToEditCipher(action.cipherId))
    }

    private fun handleLaunchCipherUrlClick(
        action: ListingItemOverflowAction.VaultAction.LaunchClick,
    ) {
        sendEvent(VaultItemListingEvent.NavigateToUrl(action.url))
    }

    private fun handleViewCipherClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(VaultItemListingEvent.NavigateToVaultItem(action.cipherId))
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
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
            event = VaultItemListingEvent.NavigateToSearchScreen(
                searchType = state.itemListingType.toSearchType(),
            ),
        )
    }

    private fun handleOverflowOptionClick(action: VaultItemListingsAction.OverflowOptionClick) {
        when (val overflowAction = action.action) {
            is ListingItemOverflowAction.SendAction.CopyUrlClick -> {
                handleCopySendUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.DeleteClick -> {
                handleDeleteSendClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.EditClick -> {
                handleEditSendClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.RemovePasswordClick -> {
                handleRemoveSendPasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ShareUrlClick -> {
                handleShareSendUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyNoteClick -> {
                handleCopyNoteClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyNumberClick -> {
                handleCopyNumberClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyPasswordClick -> {
                handleCopyPasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopySecurityCodeClick -> {
                handleCopySecurityCodeClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.CopyUsernameClick -> {
                handleCopyUsernameClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.EditClick -> {
                handleEditCipherClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.LaunchClick -> {
                handleLaunchCipherUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.VaultAction.ViewClick -> {
                handleViewCipherClick(overflowAction)
            }
        }
    }

    private fun handleInternalAction(action: VaultItemListingsAction.Internal) {
        when (action) {
            is VaultItemListingsAction.Internal.PullToRefreshEnableReceive -> {
                handlePullToRefreshEnableReceive(action)
            }

            is VaultItemListingsAction.Internal.DeleteSendResultReceive -> {
                handleDeleteSendResultReceive(action)
            }

            is VaultItemListingsAction.Internal.RemovePasswordSendResultReceive -> {
                handleRemovePasswordSendResultReceive(action)
            }

            is VaultItemListingsAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemListingsAction.Internal.IconLoadingSettingReceive -> {
                handleIconsSettingReceived(action)
            }
        }
    }

    private fun handlePullToRefreshEnableReceive(
        action: VaultItemListingsAction.Internal.PullToRefreshEnableReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isPullToRefreshSettingEnabled = action.isPullToRefreshEnabled)
        }
    }

    private fun handleDeleteSendResultReceive(
        action: VaultItemListingsAction.Internal.DeleteSendResultReceive,
    ) {
        when (action.result) {
            DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(VaultItemListingEvent.ShowToast(R.string.send_deleted.asText()))
            }
        }
    }

    private fun handleRemovePasswordSendResultReceive(
        action: VaultItemListingsAction.Internal.RemovePasswordSendResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = VaultItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is RemovePasswordSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    VaultItemListingEvent.ShowToast(
                        text = R.string.send_password_removed.asText(),
                    ),
                )
            }
        }
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
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
    }

    private fun vaultLoadedReceive(vaultData: DataState.Loaded<VaultData>) {
        updateStateWithVaultData(vaultData = vaultData.data, clearDialogState = true)
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
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
        sendEvent(VaultItemListingEvent.DismissPullToRefresh)
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
                            .folderViewList
                            .toFilteredList(state.vaultFilterType),
                        collectionList = vaultData
                            .collectionViewList
                            .toFilteredList(state.vaultFilterType),
                    ),
                viewState = when (val listingType = currentState.itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault -> {
                        vaultData
                            .cipherViewList
                            .filter { cipherView ->
                                cipherView.determineListingPredicate(listingType)
                            }
                            .toFilteredList(state.vaultFilterType)
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
                            .toViewState(
                                baseWebSendUrl = state.baseWebSendUrl,
                                clock = clock,
                            )
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
    val vaultFilterType: VaultFilterType,
    val baseWebSendUrl: String,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
    val dialogState: DialogState?,
    // Internal
    private val isPullToRefreshSettingEnabled: Boolean,
    private val autofillSelectionData: AutofillSelectionData? = null,
    private val shouldFinishOnComplete: Boolean = false,
) {

    /**
     * Indicates that the pull-to-refresh should be enabled in the UI.
     */
    val isPullToRefreshEnabled: Boolean
        get() = isPullToRefreshSettingEnabled && viewState.isPullToRefreshEnabled

    /**
     * Whether or not this represents a listing screen for autofill.
     */
    val isAutofill: Boolean
        get() = autofillSelectionData != null

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

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
         * Indicates the pull-to-refresh feature should be available during the current state.
         */
        abstract val isPullToRefreshEnabled: Boolean

        /**
         * Loading state for the [VaultItemListingScreen],
         * signifying that the content is being processed.
         */
        data object Loading : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = false
        }

        /**
         * Represents a state where the [VaultItemListingScreen] has no items to display.
         */
        data object NoItems : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Content state for the [VaultItemListingScreen] showing the actual content or items.
         *
         * @property displayItemList List of items to display.
         */
        data class Content(
            val displayItemList: List<DisplayItem>,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }

        /**
         * Represents an error state for the [VaultItemListingScreen].
         *
         * @property message Error message to display.
         */
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val isPullToRefreshEnabled: Boolean get() = true
        }
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
        val extraIconList: List<IconRes>,
        val overflowOptions: List<ListingItemOverflowAction>,
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
     * Dismisses the pull-to-refresh indicator.
     */
    data object DismissPullToRefresh : VaultItemListingEvent()

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
     * Navigates to view a cipher.
     */
    data class NavigateToEditCipher(
        val cipherId: String,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the given [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : VaultItemListingEvent()

    /**
     * Navigates to the SearchScreen with the given type filter.
     */
    data class NavigateToSearchScreen(
        val searchType: SearchType,
    ) : VaultItemListingEvent()

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
     * Click to dismiss the dialog.
     */
    data object DismissDialogClick : VaultItemListingsAction()

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
     * Click on overflow option.
     */
    data class OverflowOptionClick(
        val action: ListingItemOverflowAction,
    ) : VaultItemListingsAction()

    /**
     * Click on an item.
     *
     * @property id the id of the item that has been clicked.
     */
    data class ItemClick(val id: String) : VaultItemListingsAction()

    /**
     * User has triggered a pull to refresh.
     */
    data object RefreshPull : VaultItemListingsAction()

    /**
     * Models actions that the [VaultItemListingViewModel] itself might send.
     */
    sealed class Internal : VaultItemListingsAction() {
        /**
         * Indicates that the pull to refresh feature toggle has changed.
         */
        data class PullToRefreshEnableReceive(val isPullToRefreshEnabled: Boolean) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(val result: DeleteSendResult) : Internal()

        /**
         * Indicates a result for removing the password protection from a send has been received.
         */
        data class RemovePasswordSendResultReceive(
            val result: RemovePasswordSendResult,
        ) : Internal()

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
