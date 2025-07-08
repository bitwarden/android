package com.bitwarden.authenticator.ui.authenticator.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.ui.authenticator.feature.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.util.toSharedCodesDisplayState
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.removeDiacritics
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    private val clipboardManager: BitwardenClipboardManager,
    private val authenticatorRepository: AuthenticatorRepository,
) : BaseViewModel<ItemSearchState, ItemSearchEvent, ItemSearchAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: ItemSearchState(
            searchTerm = "",
            viewState = ItemSearchState.ViewState.Empty(message = null),
        ),
) {

    init {
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
            is ItemSearchAction.BackClick -> {
                sendEvent(ItemSearchEvent.NavigateBack)
            }

            is ItemSearchAction.SearchTermChange -> {
                mutableStateFlow.update { it.copy(searchTerm = action.searchTerm) }
                recalculateViewState()
            }

            is ItemSearchAction.ItemClick -> {
                clipboardManager.setText(action.authCode)
                sendEvent(
                    event = ItemSearchEvent.ShowToast(
                        message = R.string.value_has_been_copied.asText(action.authCode),
                    ),
                )
            }

            is ItemSearchAction.Internal.AuthenticatorDataReceive -> {
                handleAuthenticatorDataReceive(action)
            }
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
                -> SharedCodesDisplayState.Codes(emptyList())

            is SharedVerificationCodesState.Success -> {
                sharedData
                    .copy(items = sharedData.items.filterAndOrganize(searchTerm = searchTerm))
                    .toSharedCodesDisplayState(alertThresholdSeconds = 7)
            }
        }

        return when {
            filteredLocalCodes.isEmpty() && sharedItemsState.isEmpty() -> {
                ItemSearchState.ViewState.Empty(
                    message = R.string.there_are_no_items_that_match_the_search.asText(),
                )
            }

            else -> {
                ItemSearchState.ViewState.Content(
                    itemList = filteredLocalCodes.map {
                        it.toDisplayItem(
                            alertThresholdSeconds = 7,
                            sharedVerificationCodesState = authenticatorRepository
                                .sharedCodesStateFlow
                                .value,
                        )
                    },
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
            val itemList: List<VerificationCodeDisplayItem>,
            val sharedItems: SharedCodesDisplayState,
        ) : ViewState() {
            /**
             * The header to display for the local codes.
             */
            val localListHeader: Text get() = R.string.local_codes.asText(itemList.size)

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
     * User updated the search term.
     */
    data class SearchTermChange(val searchTerm: String) : ItemSearchAction()

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
     * Show a toast with the given [message].
     */
    data class ShowToast(val message: Text) : ItemSearchEvent()
}

private enum class SortPriority {
    HIGH,
    LOW,
}
