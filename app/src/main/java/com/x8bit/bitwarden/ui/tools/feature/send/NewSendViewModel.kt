package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the new send screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class NewSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<NewSendState, NewSendEvent, NewSendAction>(
    initialState = savedStateHandle[KEY_STATE] ?: NewSendState(
        name = "",
        maxAccessCount = null,
        passwordInput = "",
        noteInput = "",
        isHideEmailChecked = false,
        isDeactivateChecked = false,
        selectedType = NewSendState.SendType.Text(
            input = "",
            isHideByDefaultChecked = false,
        ),
    ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: NewSendAction): Unit = when (action) {
        is NewSendAction.CloseClick -> handleCloseClick()
        is NewSendAction.SaveClick -> handleSaveClick()
        is NewSendAction.FileTypeClick -> handleFileTypeClick()
        is NewSendAction.TextTypeClick -> handleTextTypeClick()
        is NewSendAction.ChooseFileClick -> handleChooseFileClick()
        is NewSendAction.NameChange -> handleNameChange(action)
        is NewSendAction.MaxAccessCountChange -> handleMaxAccessCountChange(action)
        is NewSendAction.TextChange -> handleTextChange(action)
        is NewSendAction.NoteChange -> handleNoteChange(action)
        is NewSendAction.PasswordChange -> handlePasswordChange(action)
        is NewSendAction.HideByDefaultToggle -> handleHideByDefaultToggle(action)
        is NewSendAction.DeactivateThisSendToggle -> handleDeactivateThisSendToggle(action)
        is NewSendAction.HideMyEmailToggle -> handleHideMyEmailToggle(action)
    }

    private fun handlePasswordChange(action: NewSendAction.PasswordChange) {
        mutableStateFlow.update {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleNoteChange(action: NewSendAction.NoteChange) {
        mutableStateFlow.update {
            it.copy(noteInput = action.input)
        }
    }

    private fun handleHideMyEmailToggle(action: NewSendAction.HideMyEmailToggle) {
        mutableStateFlow.update {
            it.copy(isHideEmailChecked = action.isChecked)
        }
    }

    private fun handleDeactivateThisSendToggle(action: NewSendAction.DeactivateThisSendToggle) {
        mutableStateFlow.update {
            it.copy(isDeactivateChecked = action.isChecked)
        }
    }

    private fun handleCloseClick() = sendEvent(NewSendEvent.NavigateBack)

    private fun handleSaveClick() = sendEvent(NewSendEvent.ShowToast("Save Not Implemented"))

    private fun handleNameChange(action: NewSendAction.NameChange) {
        mutableStateFlow.update {
            it.copy(name = action.input)
        }
    }

    private fun handleFileTypeClick() {
        mutableStateFlow.update {
            it.copy(selectedType = NewSendState.SendType.File)
        }
    }

    private fun handleTextTypeClick() {
        mutableStateFlow.update {
            it.copy(selectedType = NewSendState.SendType.Text("", isHideByDefaultChecked = false))
        }
    }

    private fun handleTextChange(action: NewSendAction.TextChange) {
        val currentSendInput =
            mutableStateFlow.value.selectedType as? NewSendState.SendType.Text ?: return
        mutableStateFlow.update {
            it.copy(selectedType = currentSendInput.copy(input = action.input))
        }
    }

    private fun handleHideByDefaultToggle(action: NewSendAction.HideByDefaultToggle) {
        val currentSendInput =
            mutableStateFlow.value.selectedType as? NewSendState.SendType.Text ?: return
        mutableStateFlow.update {
            it.copy(selectedType = currentSendInput.copy(isHideByDefaultChecked = action.isChecked))
        }
    }

    private fun handleChooseFileClick() {
        // TODO: allow for file upload: BIT-1085
        sendEvent(NewSendEvent.ShowToast("Not Implemented: File Upload"))
    }

    private fun handleMaxAccessCountChange(action: NewSendAction.MaxAccessCountChange) {
        mutableStateFlow.update {
            it.copy(maxAccessCount = action.value)
        }
    }
}

/**
 * Models state for the new send screen.
 */
@Parcelize
data class NewSendState(
    val name: String,
    val selectedType: SendType,
    // Null here means "not set"
    val maxAccessCount: Int?,
    val passwordInput: String,
    val noteInput: String,
    val isHideEmailChecked: Boolean,
    val isDeactivateChecked: Boolean,
) : Parcelable {

    /**
     * Models what type the user is trying to send.
     */
    sealed class SendType : Parcelable {
        /**
         * Sending a file.
         */
        @Parcelize
        data object File : SendType()

        /**
         * Sending text.
         */
        @Parcelize
        data class Text(
            val input: String,
            val isHideByDefaultChecked: Boolean,
        ) : SendType()
    }
}

/**
 * Models events for the new send screen.
 */
sealed class NewSendEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : NewSendEvent()

    /**
     * Show Toast.
     */
    data class ShowToast(val message: String) : NewSendEvent()
}

/**
 * Models actions for the new send screen.
 */
sealed class NewSendAction {

    /**
     * User clicked the close button.
     */
    data object CloseClick : NewSendAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : NewSendAction()

    /**
     * Value of the name field was updated.
     */
    data class NameChange(val input: String) : NewSendAction()

    /**
     * User clicked the file type segmented button.
     */
    data object FileTypeClick : NewSendAction()

    /**
     * User clicked the text type segmented button.
     */
    data object TextTypeClick : NewSendAction()

    /**
     * Value of the send text field updated.
     */
    data class TextChange(val input: String) : NewSendAction()

    /**
     * Value of the password field updated.
     */
    data class PasswordChange(val input: String) : NewSendAction()

    /**
     * Value of the note text field updated.
     */
    data class NoteChange(val input: String) : NewSendAction()

    /**
     * User clicked the choose file button.
     */
    data object ChooseFileClick : NewSendAction()

    /**
     * User toggled the "hide text by default" toggle.
     */
    data class HideByDefaultToggle(val isChecked: Boolean) : NewSendAction()

    /**
     * User incremented the max access count.
     */
    data class MaxAccessCountChange(val value: Int) : NewSendAction()

    /**
     * User toggled the "hide my email" toggle.
     */
    data class HideMyEmailToggle(val isChecked: Boolean) : NewSendAction()

    /**
     * User toggled the "deactivate this send" toggle.
     */
    data class DeactivateThisSendToggle(val isChecked: Boolean) : NewSendAction()
}
