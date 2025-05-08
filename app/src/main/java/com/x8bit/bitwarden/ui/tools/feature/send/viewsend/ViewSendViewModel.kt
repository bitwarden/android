package com.x8bit.bitwarden.ui.tools.feature.send.viewsend

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the view send screen.
 */
@HiltViewModel
class ViewSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ViewSendState, ViewSendEvent, ViewSendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = savedStateHandle.toViewSendArgs()
        ViewSendState(
            sendType = args.sendType,
            sendId = args.sendId,
            viewState = ViewSendState.ViewState.Loading,
        )
    },
) {
    override fun handleAction(action: ViewSendAction) {
        when (action) {
            ViewSendAction.CloseClick -> handleCloseClick()
            ViewSendAction.EditClick -> handleEditClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(ViewSendEvent.NavigateBack)
    }

    private fun handleEditClick() {
        sendEvent(ViewSendEvent.NavigateToEdit(sendType = state.sendType, sendId = state.sendId))
    }
}

/**
 * Models state for the new send screen.
 */
@Parcelize
data class ViewSendState(
    val sendType: SendItemType,
    val sendId: String,
    val viewState: ViewState,
) : Parcelable {
    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (sendType) {
            SendItemType.FILE -> R.string.view_file_send.asText()
            SendItemType.TEXT -> R.string.view_text_send.asText()
        }

    /**
     * Whether or not the fab is visible.
     */
    val isFabVisible: Boolean get() = viewState is ViewState.Content

    /**
     * Represents the specific view states for the view send screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the view send screen.
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the view send screen, signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the view send screen.
         */
        @Parcelize
        data object Content : ViewState()
    }
}

/**
 * Models events for the view send screen.
 */
sealed class ViewSendEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : ViewSendEvent()

    /**
     * Navigate to the edit send screen for the current send.
     */
    data class NavigateToEdit(
        val sendType: SendItemType,
        val sendId: String,
    ) : ViewSendEvent()
}

/**
 * Models actions for the view send screen.
 */
sealed class ViewSendAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : ViewSendAction()

    /**
     * The user has clicked the edit button.
     */
    data object EditClick : ViewSendAction()
}
