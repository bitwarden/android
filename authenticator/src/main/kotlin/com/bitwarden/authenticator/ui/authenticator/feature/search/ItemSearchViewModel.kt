package com.bitwarden.authenticator.ui.authenticator.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.itemsOrEmpty
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.data.platform.util.SpecialCharWithPrecedenceComparator
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.removeDiacritics
import com.bitwarden.authenticator.ui.platform.components.model.IconData
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
            val allItems = localItems + action.sharedData.itemsOrEmpty
            updateStateWithAuthenticatorData(allItems)
        }
    }

    //region Utility Functions
    private fun recalculateViewState() {
        authenticatorRepository.getLocalVerificationCodesFlow()
            .value
            .data
            ?.let { authenticatorData ->
                val allItems = authenticatorData +
                    authenticatorRepository.sharedCodesStateFlow.value.itemsOrEmpty
                updateStateWithAuthenticatorData(
                    authenticatorData = allItems,
                )
            }
    }

    private fun updateStateWithAuthenticatorData(
        authenticatorData: List<VerificationCodeItem>,
    ) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                searchTerm = currentState.searchTerm,
                viewState = authenticatorData
                    .filterAndOrganize(state.searchTerm)
                    .toViewState(searchTerm = state.searchTerm),
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
        val itemName = otpAuthUriLabel?.removeDiacritics()
        val itemId = id.takeIf { term.length > 8 }.orEmpty().removeDiacritics()
        val itemIssuer = issuer.orEmpty().removeDiacritics()
        return when {
            itemName?.contains(other = term, ignoreCase = true) ?: false -> SortPriority.HIGH
            itemId.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            itemIssuer.contains(other = term, ignoreCase = true) -> SortPriority.LOW
            else -> null
        }
    }

    private fun List<VerificationCodeItem>.toViewState(
        searchTerm: String,
    ): ItemSearchState.ViewState =
        when {
            searchTerm.isEmpty() -> {
                ItemSearchState.ViewState.Empty(message = null)
            }

            isNotEmpty() -> {
                ItemSearchState.ViewState.Content(
                    displayItems = toDisplayItemList()
                        .sortAlphabetically(),
                )
            }

            else -> {
                ItemSearchState.ViewState.Empty(
                    message = R.string.there_are_no_items_that_match_the_search.asText(),
                )
            }
        }

    private fun List<VerificationCodeItem>.toDisplayItemList(): List<ItemSearchState.DisplayItem> =
        this.map {
            it.toDisplayItem()
        }

    private fun VerificationCodeItem.toDisplayItem(): ItemSearchState.DisplayItem =
        ItemSearchState.DisplayItem(
            id = id,
            authCode = code,
            title = issuer ?: label ?: "--",
            subtitle = if (issuer != null) {
                // Only show label if it is not being used as the primary title:
                label
            } else {
                null
            },
            periodSeconds = periodSeconds,
            timeLeftSeconds = timeLeftSeconds,
            alertThresholdSeconds = 7,
            startIcon = IconData.Local(iconRes = R.drawable.ic_login_item),
        )

    /**
     * Sort a list of [ItemSearchState.DisplayItem] by their titles alphabetically giving digits and
     * special characters higher precedence.
     */
    @Suppress("MaxLineLength")
    private fun List<ItemSearchState.DisplayItem>.sortAlphabetically() =
        this.sortedWith { item1, item2 ->
            SpecialCharWithPrecedenceComparator.compare(
                item1.title.orEmpty(),
                item2.title.orEmpty(),
            )
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
            val displayItems: List<DisplayItem>,
        ) : ViewState()

        /**
         * Show the empty state.
         */
        @Parcelize
        data class Empty(val message: Text?) : ViewState()
    }

    /**
     * An item to be displayed.
     */
    @Parcelize
    data class DisplayItem(
        val id: String,
        val authCode: String,
        val title: String,
        val subtitle: String? = null,
        val periodSeconds: Int,
        val timeLeftSeconds: Int,
        val alertThresholdSeconds: Int,
        val startIcon: IconData,
    ) : Parcelable
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
