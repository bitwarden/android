package com.bitwarden.authenticator.ui.authenticator.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.ui.authenticator.feature.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.util.toSharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.removeDiacritics
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the item search screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ItemSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val clipboardManager: BitwardenClipboardManager,
    private val authenticatorRepository: AuthenticatorRepository,
    private val authenticatorBridgeManager: AuthenticatorBridgeManager,
) : BaseViewModel<ItemSearchState, ItemSearchEvent, ItemSearchAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ItemSearchState(
            searchTerm = "",
            viewState = ItemSearchState.ViewState.Empty(message = null),
            dialog = null,
        ),
) {

    init {
        snackbarRelayManager
            .getSnackbarDataFlow(relay = SnackbarRelay.ITEM_SAVED)
            .map(ItemSearchEvent::ShowSnackbar)
            .onEach(::sendEvent)
            .launchIn(viewModelScope)
        combine(
            authenticatorRepository.getLocalVerificationCodesFlow(),
            authenticatorRepository.sharedCodesStateFlow,
        ) { localItems, sharedItems ->
            ItemSearchAction.Internal.AuthenticatorDataReceive(localItems, sharedItems)
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemSearchAction) {
        when (action) {
            is ItemSearchAction.BackClick -> handleBackClick()
            is ItemSearchAction.ConfirmDeleteClick -> handleConfirmDeleteClick(action)
            is ItemSearchAction.DismissDialog -> handleDismissDialog()
            is ItemSearchAction.SearchTermChange -> handleSearchTermChange(action)
            is ItemSearchAction.ItemClick -> handleItemClick(action)
            is ItemSearchAction.DropdownMenuClick -> handleDropdownMenuClick(action)
            is ItemSearchAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(ItemSearchEvent.NavigateBack)
    }

    private fun handleConfirmDeleteClick(action: ItemSearchAction.ConfirmDeleteClick) {
        mutableStateFlow.update {
            it.copy(dialog = ItemSearchState.DialogState.Loading)
        }

        viewModelScope.launch {
            trySendAction(
                ItemSearchAction.Internal.DeleteItemReceive(
                    result = authenticatorRepository.hardDeleteItem(action.itemId),
                ),
            )
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleSearchTermChange(action: ItemSearchAction.SearchTermChange) {
        mutableStateFlow.update { it.copy(searchTerm = action.searchTerm) }
        recalculateViewState()
    }

    private fun handleItemClick(action: ItemSearchAction.ItemClick) {
        clipboardManager.setText(action.authCode)
    }

    private fun handleDropdownMenuClick(action: ItemSearchAction.DropdownMenuClick) {
        when (action.menuAction) {
            VaultDropdownMenuAction.COPY_CODE -> clipboardManager.setText(action.item.authCode)
            VaultDropdownMenuAction.COPY_TO_BITWARDEN -> {
                viewModelScope.launch {
                    val item = authenticatorRepository
                        .getItemStateFlow(itemId = action.item.id)
                        .first { it.data != null }
                    val isSuccess = authenticatorBridgeManager.startAddTotpLoginItemFlow(
                        totpUri = item.data?.toOtpAuthUriString().orEmpty(),
                    )
                    sendAction(ItemSearchAction.Internal.AddTotpLoginItemFlowResult(isSuccess))
                }
            }

            VaultDropdownMenuAction.EDIT -> {
                sendEvent(ItemSearchEvent.NavigateToEditItem(action.item.id))
            }

            VaultDropdownMenuAction.DELETE -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemSearchState.DialogState.DeleteConfirmationPrompt(
                            message = BitwardenString
                                .do_you_really_want_to_permanently_delete_this_cannot_be_undone
                                .asText(),
                            itemId = action.item.id,
                        ),
                    )
                }
            }
        }
    }

    private fun handleInternalAction(action: ItemSearchAction.Internal) {
        when (action) {
            is ItemSearchAction.Internal.AuthenticatorDataReceive -> {
                handleAuthenticatorDataReceive(action)
            }

            is ItemSearchAction.Internal.AddTotpLoginItemFlowResult -> {
                handleAddTotpLoginItemFlowResult(action)
            }

            is ItemSearchAction.Internal.DeleteItemReceive -> handleDeleteItemReceive(action)
        }
    }

    private fun handleAuthenticatorDataReceive(
        action: ItemSearchAction.Internal.AuthenticatorDataReceive,
    ) {
        action.localData.data?.let { localItems ->
            updateStateWithAuthenticatorData(
                localCodes = localItems,
                sharedData = action.sharedData,
            )
        }
    }

    private fun handleAddTotpLoginItemFlowResult(
        action: ItemSearchAction.Internal.AddTotpLoginItemFlowResult,
    ) {
        if (action.isSuccess) return
        mutableStateFlow.update {
            it.copy(
                dialog = ItemSearchState.DialogState.Error(
                    title = BitwardenString.something_went_wrong.asText(),
                    message = BitwardenString.please_try_again.asText(),
                ),
            )
        }
    }

    private fun handleDeleteItemReceive(action: ItemSearchAction.Internal.DeleteItemReceive) {
        when (action.result) {
            DeleteItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemSearchState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteItemResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                sendEvent(ItemSearchEvent.ShowSnackbar(BitwardenString.item_deleted.asText()))
            }
        }
    }

    //region Utility Functions
    private fun recalculateViewState() {
        authenticatorRepository.getLocalVerificationCodesFlow()
            .value
            .data
            ?.let { authenticatorData ->
                updateStateWithAuthenticatorData(
                    localCodes = authenticatorData,
                    sharedData = authenticatorRepository.sharedCodesStateFlow.value,
                )
            }
    }

    private fun updateStateWithAuthenticatorData(
        localCodes: List<VerificationCodeItem>,
        sharedData: SharedVerificationCodesState,
    ) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                viewState = toViewState(
                    searchTerm = state.searchTerm,
                    localCodes = localCodes,
                    sharedData = sharedData,
                ),
            )
        }
    }

    private fun List<VerificationCodeItem>.filterAndOrganize(searchTerm: String) =
        if (searchTerm.isBlank()) {
            emptyList()
        } else {
            this
                .groupBy { it.matchedSearch(searchTerm) }
                .flatMap { (priority, items) ->
                    when (priority) {
                        SortPriority.HIGH -> items.sortedBy { it.otpAuthUriLabel }
                        SortPriority.LOW -> items.sortedBy { it.otpAuthUriLabel }
                        null -> emptyList()
                    }
                }
        }

    @Suppress("MagicNumber")
    private fun VerificationCodeItem.matchedSearch(searchTerm: String): SortPriority? {
        val term = searchTerm.removeDiacritics()
        val itemName = otpAuthUriLabel.removeDiacritics()
        val itemId = id.takeIf { term.length > 8 }.orEmpty().removeDiacritics()
        val itemIssuer = issuer.orEmpty().removeDiacritics()
        return when {
            itemName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
            itemId.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            itemIssuer.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            else -> null
        }
    }

    private fun toViewState(
        searchTerm: String,
        localCodes: List<VerificationCodeItem>,
        sharedData: SharedVerificationCodesState,
    ): ItemSearchState.ViewState {
        if (searchTerm.isEmpty()) {
            return ItemSearchState.ViewState.Empty(message = null)
        }

        val filteredLocalCodes = localCodes.filterAndOrganize(searchTerm = searchTerm)
        val sharedItemsState = when (sharedData) {
            SharedVerificationCodesState.Error -> SharedCodesDisplayState.Error
            SharedVerificationCodesState.AppNotInstalled,
            SharedVerificationCodesState.FeatureNotEnabled,
            SharedVerificationCodesState.Loading,
            SharedVerificationCodesState.OsVersionNotSupported,
            SharedVerificationCodesState.SyncNotEnabled,
                -> SharedCodesDisplayState.Codes(persistentListOf())

            is SharedVerificationCodesState.Success -> {
                sharedData
                    .copy(items = sharedData.items.filterAndOrganize(searchTerm = searchTerm))
                    .toSharedCodesDisplayState(alertThresholdSeconds = 7)
            }
        }

        return when {
            filteredLocalCodes.isEmpty() && sharedItemsState.isEmpty() -> {
                ItemSearchState.ViewState.Empty(
                    message = BitwardenString.there_are_no_items_that_match_the_search.asText(),
                )
            }

            else -> {
                ItemSearchState.ViewState.Content(
                    itemList = filteredLocalCodes
                        .map {
                            it.toDisplayItem(
                                alertThresholdSeconds = 7,
                                sharedVerificationCodesState = authenticatorRepository
                                    .sharedCodesStateFlow
                                    .value,
                                showOverflow = true,
                            )
                        }
                        .toImmutableList(),
                    sharedItems = sharedItemsState,
                )
            }
        }
    }
    //endregion Utility Functions
}

/**
 * Represents the overall state for the [ItemSearchScreen].
 */
@Parcelize
data class ItemSearchState(
    val searchTerm: String,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {
    /**
     * Represents the specific view state for the search screen.
     */
    sealed class ViewState : Parcelable {

        /**
         * Show the populated state.
         */
        @Parcelize
        data class Content(
            val itemList: ImmutableList<VerificationCodeDisplayItem>,
            val sharedItems: SharedCodesDisplayState,
        ) : ViewState() {
            /**
             * The header to display for the local codes.
             */
            val localListHeader: Text get() = BitwardenString.local_codes.asText(itemList.size)

            /**
             * Whether or not there should be a "Local codes" header shown above local codes.
             */
            val hasLocalAndSharedItems get() = !sharedItems.isEmpty() && itemList.isNotEmpty()
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data class Empty(val message: Text?) : ViewState()
    }

    /**
     * Display a dialog on the [ItemSearchScreen].
     */
    sealed class DialogState : Parcelable {
        /**
         * Displays a prompt to confirm item deletion.
         */
        @Parcelize
        data class DeleteConfirmationPrompt(
            val message: Text,
            val itemId: String,
        ) : DialogState()

        /**
         * Displays the loading dialog to the user.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * Displays a generic error dialog to the user.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()
    }
}

/**
 * Models actions for the [ItemSearchScreen].
 */
sealed class ItemSearchAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : ItemSearchAction()

    /**
     * User has dismissed a dialog.
     */
    data object DismissDialog : ItemSearchAction()

    /**
     * User updated the search term.
     */
    data class SearchTermChange(val searchTerm: String) : ItemSearchAction()

    /**
     * The user clicked confirm when prompted to delete an item.
     */
    data class ConfirmDeleteClick(val itemId: String) : ItemSearchAction()

    /**
     * Represents an action triggered when the user clicks an item in the dropdown menu.
     */
    data class DropdownMenuClick(
        val menuAction: VaultDropdownMenuAction,
        val item: VerificationCodeDisplayItem,
    ) : ItemSearchAction()

    /**
     * User clicked a row item.
     */
    data class ItemClick(val authCode: String) : ItemSearchAction()

    /**
     * Models actions that the [ItemSearchViewModel] itself might send.
     */
    sealed class Internal : ItemSearchAction() {

        /**
         * Indicates authenticate data was received.
         */
        data class AuthenticatorDataReceive(
            val localData: DataState<List<VerificationCodeItem>>,
            val sharedData: SharedVerificationCodesState,
        ) : Internal()

        /**
         * Indicates the result of the add totp login item flow.
         */
        data class AddTotpLoginItemFlowResult(
            val isSuccess: Boolean,
        ) : Internal()

        /**
         * Indicates a result for deleting an item has been received.
         */
        data class DeleteItemReceive(
            val result: DeleteItemResult,
        ) : Internal()
    }
}

/**
 * Models events for the [ItemSearchScreen].
 */
sealed class ItemSearchEvent {

    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : ItemSearchEvent()

    /**
     * Navigate to the edit item screen.
     */
    data class NavigateToEditItem(val itemId: String) : ItemSearchEvent()

    /**
     * Show a Snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : ItemSearchEvent(), BackgroundEvent {
        constructor(
            message: Text,
            messageHeader: Text? = null,
            actionLabel: Text? = null,
            withDismissAction: Boolean = false,
        ) : this(
            data = BitwardenSnackbarData(
                message = message,
                messageHeader = messageHeader,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            ),
        )
    }
}

private enum class SortPriority {
    HIGH,
    LOW,
}
