package com.x8bit.bitwarden.ui.platform.feature.search

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the search screen.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SearchState, SearchEvent, SearchAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: SearchState(
            searchType = SearchArgs(savedStateHandle).type,
        ),
) {
    override fun handleAction(action: SearchAction) {
        when (action) {
            SearchAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(SearchEvent.NavigateBack)
    }
}

/**
 * Represents the overall state for the [SearchScreen].
 */
@Parcelize
data class SearchState(
    val searchType: SearchType,
) : Parcelable

/**
 * Models actions for the [SearchScreen].
 */
sealed class SearchAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : SearchAction()
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
