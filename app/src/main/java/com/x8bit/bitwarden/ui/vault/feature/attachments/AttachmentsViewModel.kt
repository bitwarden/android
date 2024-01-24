package com.x8bit.bitwarden.ui.vault.feature.attachments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the attachments screen.
 */
@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AttachmentsState, AttachmentsEvent, AttachmentsAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: AttachmentsState(
            cipherId = AttachmentsArgs(savedStateHandle).cipherId,
        ),
) {
    override fun handleAction(action: AttachmentsAction) {
        when (action) {
            AttachmentsAction.BackClick -> handleBackClick()
            AttachmentsAction.SaveClick -> handleSaveClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(AttachmentsEvent.NavigateBack)
    }

    private fun handleSaveClick() {
        sendEvent(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()))
        // TODO: Handle saving the attachments (BIT-522)
    }
}

/**
 * Represents the state for viewing attachments.
 */
@Parcelize
data class AttachmentsState(
    val cipherId: String,
) : Parcelable

/**
 * Represents a set of events related attachments.
 */
sealed class AttachmentsEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : AttachmentsEvent()

    /**
     * Displays the given [message] as a toast.
     */
    data class ShowToast(
        val message: Text,
    ) : AttachmentsEvent()
}

/**
 * Represents a set of actions related to attachments.
 */
sealed class AttachmentsAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : AttachmentsAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : AttachmentsAction()
}
