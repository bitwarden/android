package com.x8bit.bitwarden.ui.platform.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.GenerateTotpResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.search.util.filterAndOrganize
import com.x8bit.bitwarden.ui.platform.feature.search.util.toSearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.util.toViewState
import com.x8bit.bitwarden.ui.platform.feature.search.util.updateWithAdditionalDataIfNecessary
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toVaultFilterData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the search screen.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val clipboardManager: BitwardenClipboardManager,
    private val vaultRepo: VaultRepository,
    authRepo: AuthRepository,
    environmentRepo: EnvironmentRepository,
    settingsRepo: SettingsRepository,
) : BaseViewModel<SearchState, SearchEvent, SearchAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val searchType = SearchArgs(savedStateHandle).type
            val userState = requireNotNull(authRepo.userStateFlow.value)
            SearchState(
                searchTerm = "",
                searchType = searchType.toSearchTypeData(),
                viewState = SearchState.ViewState.Loading,
                dialogState = null,
                vaultFilterData = when (searchType) {
                    is SearchType.Sends -> null
                    is SearchType.Vault -> userState.activeAccount.toVaultFilterData()
                },
                baseWebSendUrl = environmentRepo.environment.environmentUrlData.baseWebSendUrl,
                baseIconUrl = environmentRepo.environment.environmentUrlData.baseIconUrl,
                isIconLoadingDisabled = settingsRepo.isIconLoadingDisabled,
            )
        },
) {
    init {
        settingsRepo
            .isIconLoadingDisabledFlow
            .map { SearchAction.Internal.IconLoadingSettingReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        vaultRepo
            .vaultDataStateFlow
            .map { SearchAction.Internal.VaultDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SearchAction) {
        when (action) {
            SearchAction.BackClick -> handleBackClick()
            SearchAction.DismissDialogClick -> handleDismissClick()
            is SearchAction.ItemClick -> handleItemClick(action)
            is SearchAction.SearchTermChange -> handleSearchTermChange(action)
            is SearchAction.VaultFilterSelect -> handleVaultFilterSelect(action)
            is SearchAction.OverflowOptionClick -> handleOverflowItemClick(action)
            is SearchAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(SearchEvent.NavigateBack)
    }

    private fun handleDismissClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleItemClick(action: SearchAction.ItemClick) {
        val event = when (state.searchType) {
            is SearchTypeData.Vault -> {
                SearchEvent.NavigateToViewCipher(cipherId = action.itemId)
            }

            is SearchTypeData.Sends -> {
                SearchEvent.NavigateToEditSend(sendId = action.itemId)
            }
        }
        sendEvent(event)
    }

    private fun handleSearchTermChange(action: SearchAction.SearchTermChange) {
        mutableStateFlow.update { it.copy(searchTerm = action.searchTerm) }
        recalculateViewState()
    }

    private fun handleVaultFilterSelect(action: SearchAction.VaultFilterSelect) {
        mutableStateFlow.update {
            it.copy(
                vaultFilterData = it.vaultFilterData?.copy(
                    selectedVaultFilterType = action.vaultFilterType,
                ),
            )
        }
        recalculateViewState()
    }

    private fun handleOverflowItemClick(action: SearchAction.OverflowOptionClick) {
        when (val overflowAction = action.overflowAction) {
            is ListingItemOverflowAction.SendAction.CopyUrlClick -> {
                handleCopyUrlClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.DeleteClick -> {
                handleDeleteClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.EditClick -> handleEditClick(overflowAction)
            is ListingItemOverflowAction.SendAction.RemovePasswordClick -> {
                handleRemovePasswordClick(overflowAction)
            }

            is ListingItemOverflowAction.SendAction.ShareUrlClick -> {
                handleShareUrlClick(overflowAction)
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

            is ListingItemOverflowAction.VaultAction.CopyTotpClick -> {
                handleCopyTotpClick(overflowAction)
            }
        }
    }

    private fun handleCopyUrlClick(action: ListingItemOverflowAction.SendAction.CopyUrlClick) {
        clipboardManager.setText(action.sendUrl)
    }

    private fun handleDeleteClick(action: ListingItemOverflowAction.SendAction.DeleteClick) {
        mutableStateFlow.update {
            it.copy(dialogState = SearchState.DialogState.Loading(R.string.deleting.asText()))
        }
        viewModelScope.launch {
            val result = vaultRepo.deleteSend(action.sendId)
            sendAction(SearchAction.Internal.DeleteSendResultReceive(result))
        }
    }

    private fun handleEditClick(action: ListingItemOverflowAction.SendAction.EditClick) {
        sendEvent(SearchEvent.NavigateToEditSend(action.sendId))
    }

    private fun handleCopyTotpClick(
        action: ListingItemOverflowAction.VaultAction.CopyTotpClick,
    ) {
        viewModelScope.launch {
            val result = vaultRepo.generateTotp(action.totpCode, clock.instant())
            sendAction(SearchAction.Internal.GenerateTotpResultReceive(result))
        }
    }

    private fun handleRemovePasswordClick(
        action: ListingItemOverflowAction.SendAction.RemovePasswordClick,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialogState = SearchState.DialogState.Loading(
                    message = R.string.removing_send_password.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = vaultRepo.removePasswordSend(action.sendId)
            sendAction(SearchAction.Internal.RemovePasswordSendResultReceive(result))
        }
    }

    private fun handleShareUrlClick(action: ListingItemOverflowAction.SendAction.ShareUrlClick) {
        sendEvent(SearchEvent.ShowShareSheet(action.sendUrl))
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
        sendEvent(SearchEvent.NavigateToEditCipher(action.cipherId))
    }

    private fun handleLaunchCipherUrlClick(
        action: ListingItemOverflowAction.VaultAction.LaunchClick,
    ) {
        sendEvent(SearchEvent.NavigateToUrl(action.url))
    }

    private fun handleViewCipherClick(action: ListingItemOverflowAction.VaultAction.ViewClick) {
        sendEvent(SearchEvent.NavigateToViewCipher(action.cipherId))
    }

    private fun handleInternalAction(action: SearchAction.Internal) {
        when (action) {
            is SearchAction.Internal.IconLoadingSettingReceive -> {
                handleIconLoadingSettingReceive(action)
            }

            is SearchAction.Internal.DeleteSendResultReceive -> {
                handleDeleteSendResultReceive(action)
            }

            is SearchAction.Internal.GenerateTotpResultReceive -> {
                handleGenerateTotpResultReceive(action)
            }

            is SearchAction.Internal.RemovePasswordSendResultReceive -> {
                handleRemovePasswordSendResultReceive(action)
            }

            is SearchAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
        }
    }

    private fun handleIconLoadingSettingReceive(
        action: SearchAction.Internal.IconLoadingSettingReceive,
    ) {
        mutableStateFlow.update { it.copy(isIconLoadingDisabled = action.isIconLoadingDisabled) }
        recalculateViewState()
    }

    private fun handleDeleteSendResultReceive(
        action: SearchAction.Internal.DeleteSendResultReceive,
    ) {
        when (action.result) {
            DeleteSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(SearchEvent.ShowToast(R.string.send_deleted.asText()))
            }
        }
    }

    private fun handleGenerateTotpResultReceive(
        action: SearchAction.Internal.GenerateTotpResultReceive,
    ) {
        when (val result = action.result) {
            is GenerateTotpResult.Error -> Unit
            is GenerateTotpResult.Success -> {
                clipboardManager.setText(result.code)
            }
        }
    }

    private fun handleRemovePasswordSendResultReceive(
        action: SearchAction.Internal.RemovePasswordSendResultReceive,
    ) {
        when (val result = action.result) {
            is RemovePasswordSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = SearchState.DialogState.Error(
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
                sendEvent(SearchEvent.ShowToast(R.string.send_password_removed.asText()))
            }
        }
    }

    private fun handleVaultDataReceive(
        action: SearchAction.Internal.VaultDataReceive,
    ) {
        when (val vaultData = action.vaultData) {
            is DataState.Error -> vaultErrorReceive(vaultData = vaultData)
            is DataState.Loaded -> vaultLoadedReceive(vaultData = vaultData)
            is DataState.Loading -> vaultLoadingReceive()
            is DataState.NoNetwork -> vaultNoNetworkReceive(vaultData = vaultData)
            is DataState.Pending -> vaultPendingReceive(vaultData = vaultData)
        }
    }

    private fun vaultErrorReceive(vaultData: DataState.Error<VaultData>) {
        vaultData
            .data
            ?.let { updateStateWithVaultData(vaultData = it, clearDialogState = true) }
            ?: run {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SearchState.ViewState.Error(
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
        mutableStateFlow.update { it.copy(viewState = SearchState.ViewState.Loading) }
    }

    private fun vaultNoNetworkReceive(vaultData: DataState.NoNetwork<VaultData>) {
        vaultData
            .data
            ?.let { updateStateWithVaultData(vaultData = it, clearDialogState = true) }
            ?: run {
                mutableStateFlow.update { currentState ->
                    currentState.copy(
                        viewState = SearchState.ViewState.Error(
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

    private fun recalculateViewState() {
        vaultRepo.vaultDataStateFlow.value.data?.let { vaultData ->
            updateStateWithVaultData(vaultData = vaultData, clearDialogState = false)
        }
    }

    private fun updateStateWithVaultData(vaultData: VaultData, clearDialogState: Boolean) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                searchType = currentState
                    .searchType
                    .updateWithAdditionalDataIfNecessary(
                        folderList = vaultData.folderViewList,
                        collectionList = vaultData.collectionViewList,
                    ),
                viewState = when (val searchType = currentState.searchType) {
                    is SearchTypeData.Vault -> {
                        vaultData
                            .cipherViewList
                            .filterAndOrganize(searchType, state.searchTerm)
                            .toFilteredList(
                                vaultFilterType = state
                                    .vaultFilterData
                                    ?.selectedVaultFilterType
                                    ?: VaultFilterType.AllVaults,
                            )
                            .toViewState(
                                searchTerm = state.searchTerm,
                                baseIconUrl = state.baseIconUrl,
                                isIconLoadingDisabled = state.isIconLoadingDisabled,
                            )
                    }

                    is SearchTypeData.Sends -> {
                        vaultData
                            .sendViewList
                            .filterAndOrganize(searchType, state.searchTerm)
                            .toViewState(
                                searchTerm = state.searchTerm,
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
 * Represents the overall state for the [SearchScreen].
 */
@Parcelize
data class SearchState(
    val searchTerm: String,
    val searchType: SearchTypeData,
    val viewState: ViewState,
    val dialogState: DialogState?,
    val vaultFilterData: VaultFilterData?,
    val baseWebSendUrl: String,
    val baseIconUrl: String,
    val isIconLoadingDisabled: Boolean,
) : Parcelable {
    /**
     * Represents the specific view states for the search screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Determines whether or not the the Vault Filter may be shown (when applicable).
         */
        abstract val hasVaultFilter: Boolean

        /**
         * Show the populated state.
         */
        @Parcelize
        data class Content(
            val displayItems: List<DisplayItem>,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = true
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data class Empty(
            val message: Text?,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = true
        }

        /**
         * Show the error state.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val hasVaultFilter: Boolean get() = false
        }

        /**
         * Show the loading state.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val hasVaultFilter: Boolean get() = false
        }
    }

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
     * An item to be displayed.
     */
    @Parcelize
    data class DisplayItem(
        val id: String,
        val title: String,
        val subtitle: String?,
        val totpCode: String?,
        val iconData: IconData,
        val extraIconList: List<IconRes>,
        val overflowOptions: List<ListingItemOverflowAction>,
    ) : Parcelable
}

/**
 * Represents the difference between searching sends and searching vault items.
 */
sealed class SearchTypeData : Parcelable {
    /**
     * The hint to display in the search toolbar at the top of the screen.
     */
    abstract val title: Text

    /**
     * Indicates that we should be searching sends.
     */
    @Parcelize
    sealed class Sends : SearchTypeData() {
        /**
         * Indicates that we should be searching all sends.
         */
        data object All : Sends() {
            override val title: Text get() = R.string.search_sends.asText()
        }

        /**
         * Indicates that we should be searching only text sends.
         */
        data object Texts : Sends() {
            override val title: Text get() = R.string.search_text_sends.asText()
        }

        /**
         * Indicates that we should be searching only file sends.
         */
        data object Files : Sends() {
            override val title: Text get() = R.string.search_file_sends.asText()
        }
    }

    /**
     * Indicates that we should be searching vault items.
     */
    @Parcelize
    sealed class Vault : SearchTypeData() {
        /**
         * Indicates that we should be searching all vault items.
         */
        data object All : Vault() {
            override val title: Text get() = R.string.search_vault.asText()
        }

        /**
         * Indicates that we should be searching only login ciphers.
         */
        data object Logins : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.logins.asText())
        }

        /**
         * Indicates that we should be searching only card ciphers.
         */
        data object Cards : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.cards.asText())
        }

        /**
         * Indicates that we should be searching only identity ciphers.
         */
        data object Identities : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.identities.asText())
        }

        /**
         * Indicates that we should be searching only secure note ciphers.
         */
        data object SecureNotes : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.secure_notes.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the given collection.
         */
        data class Collection(
            val collectionId: String,
            val collectionName: String = "",
        ) : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(collectionName.asText())
        }

        /**
         * Indicates that we should be searching only ciphers not in a folder.
         */
        data object NoFolder : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.folder_none.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the given folder.
         */
        data class Folder(
            val folderId: String,
            val folderName: String = "",
        ) : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(folderName.asText())
        }

        /**
         * Indicates that we should be searching only ciphers in the trash.
         */
        data object Trash : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.trash.asText())
        }

        /**
         * Indicates that we should be searching only for verification code items.
         */
        data object VerificationCodes : Vault() {
            override val title: Text
                get() = R.string.search.asText()
                    .concat(" ".asText())
                    .concat(R.string.verification_codes.asText())
        }
    }
}

/**
 * Models actions for the [SearchScreen].
 */
sealed class SearchAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : SearchAction()

    /**
     * User clicked to dismiss the dialog.
     */
    data object DismissDialogClick : SearchAction()

    /**
     * User clicked a row item.
     */
    data class ItemClick(
        val itemId: String,
    ) : SearchAction()

    /**
     * User updated the search term.
     */
    data class SearchTermChange(
        val searchTerm: String,
    ) : SearchAction()

    /**
     * User selected a new vault filter type.
     */
    data class VaultFilterSelect(
        val vaultFilterType: VaultFilterType,
    ) : SearchAction()

    /**
     * User clicked on an overflow action.
     */
    data class OverflowOptionClick(
        val overflowAction: ListingItemOverflowAction,
    ) : SearchAction()

    /**
     * Models actions that the [SearchViewModel] itself might send.
     */
    sealed class Internal : SearchAction() {
        /**
         * Indicates the icon setting was received.
         */
        data class IconLoadingSettingReceive(
            val isIconLoadingDisabled: Boolean,
        ) : Internal()

        /**
         * Indicates a result for deleting the send has been received.
         */
        data class DeleteSendResultReceive(
            val result: DeleteSendResult,
        ) : Internal()

        /**
         * Indicates a result for generating a verification code has been received.
         */
        data class GenerateTotpResultReceive(
            val result: GenerateTotpResult,
        ) : Internal()

        /**
         * Indicates a result for removing the password protection from a send has been received.
         */
        data class RemovePasswordSendResultReceive(
            val result: RemovePasswordSendResult,
        ) : Internal()

        /**
         * Indicates vault data was received.
         */
        data class VaultDataReceive(
            val vaultData: DataState<VaultData>,
        ) : Internal()
    }
}

/**
 * Models events for the [SearchScreen].
 */
sealed class SearchEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : SearchEvent()

    /**
     * Navigates to edit a send.
     */
    data class NavigateToEditSend(
        val sendId: String,
    ) : SearchEvent()

    /**
     * Navigates to view a cipher.
     */
    data class NavigateToEditCipher(
        val cipherId: String,
    ) : SearchEvent()

    /**
     * Navigates to edit a cipher.
     */
    data class NavigateToViewCipher(
        val cipherId: String,
    ) : SearchEvent()

    /**
     * Navigates to the given [url].
     */
    data class NavigateToUrl(
        val url: String,
    ) : SearchEvent()

    /**
     * Shares the [content] with share sheet.
     */
    data class ShowShareSheet(
        val content: String,
    ) : SearchEvent()

    /**
     * Show a toast with the given [message].
     */
    data class ShowToast(
        val message: Text,
    ) : SearchEvent()
}
