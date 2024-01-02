package com.x8bit.bitwarden.ui.tools.feature.send.addsend

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
) : BaseViewModel<AddSendState, AddSendEvent, AddSendAction>(
    initialState = savedStateHandle[KEY_STATE] ?: AddSendState(
        name = "",
        maxAccessCount = null,
        passwordInput = "",
        noteInput = "",
        isHideEmailChecked = false,
        isDeactivateChecked = false,
        selectedType = AddSendState.SendType.Text(
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

    override fun handleAction(action: AddSendAction): Unit = when (action) {
        is AddSendAction.CloseClick -> handleCloseClick()
        is AddSendAction.SaveClick -> handleSaveClick()
        is AddSendAction.FileTypeClick -> handleFileTypeClick()
        is AddSendAction.TextTypeClick -> handleTextTypeClick()
        is AddSendAction.ChooseFileClick -> handleChooseFileClick()
        is AddSendAction.NameChange -> handleNameChange(action)
        is AddSendAction.MaxAccessCountChange -> handleMaxAccessCountChange(action)
        is AddSendAction.TextChange -> handleTextChange(action)
        is AddSendAction.NoteChange -> handleNoteChange(action)
        is AddSendAction.PasswordChange -> handlePasswordChange(action)
        is AddSendAction.HideByDefaultToggle -> handleHideByDefaultToggle(action)
        is AddSendAction.DeactivateThisSendToggle -> handleDeactivateThisSendToggle(action)
        is AddSendAction.HideMyEmailToggle -> handleHideMyEmailToggle(action)
    }

    private fun handlePasswordChange(action: AddSendAction.PasswordChange) {
        mutableStateFlow.update {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleNoteChange(action: AddSendAction.NoteChange) {
        mutableStateFlow.update {
            it.copy(noteInput = action.input)
        }
    }

    private fun handleHideMyEmailToggle(action: AddSendAction.HideMyEmailToggle) {
        mutableStateFlow.update {
            it.copy(isHideEmailChecked = action.isChecked)
        }
    }

    private fun handleDeactivateThisSendToggle(action: AddSendAction.DeactivateThisSendToggle) {
        mutableStateFlow.update {
            it.copy(isDeactivateChecked = action.isChecked)
        }
    }

    private fun handleCloseClick() = sendEvent(AddSendEvent.NavigateBack)

    private fun handleSaveClick() = sendEvent(AddSendEvent.ShowToast("Save Not Implemented"))

    private fun handleNameChange(action: AddSendAction.NameChange) {
        mutableStateFlow.update {
            it.copy(name = action.input)
        }
    }

    private fun handleFileTypeClick() {
        mutableStateFlow.update {
            it.copy(selectedType = AddSendState.SendType.File)
        }
    }

    private fun handleTextTypeClick() {
        mutableStateFlow.update {
            it.copy(selectedType = AddSendState.SendType.Text("", isHideByDefaultChecked = false))
        }
    }

    private fun handleTextChange(action: AddSendAction.TextChange) {
        val currentSendInput =
            mutableStateFlow.value.selectedType as? AddSendState.SendType.Text ?: return
        mutableStateFlow.update {
            it.copy(selectedType = currentSendInput.copy(input = action.input))
        }
    }

    private fun handleHideByDefaultToggle(action: AddSendAction.HideByDefaultToggle) {
        val currentSendInput =
            mutableStateFlow.value.selectedType as? AddSendState.SendType.Text ?: return
        mutableStateFlow.update {
            it.copy(selectedType = currentSendInput.copy(isHideByDefaultChecked = action.isChecked))
        }
    }

    private fun handleChooseFileClick() {
        // TODO: allow for file upload: BIT-1085
        sendEvent(AddSendEvent.ShowToast("Not Implemented: File Upload"))
    }

    private fun handleMaxAccessCountChange(action: AddSendAction.MaxAccessCountChange) {
        mutableStateFlow.update {
            it.copy(maxAccessCount = action.value)
        }
    }
}

/**
 * Models state for the new send screen.
 */
@Parcelize
data class AddSendState(
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
sealed class AddSendEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AddSendEvent()

    /**
     * Show Toast.
     */
    data class ShowToast(val message: String) : AddSendEvent()
}

/**
 * Models actions for the new send screen.
 */
sealed class AddSendAction {

    /**
     * User clicked the close button.
     */
    data object CloseClick : AddSendAction()

    /**
     * User clicked the save button.
     */
    data object SaveClick : AddSendAction()

    /**
     * Value of the name field was updated.
     */
    data class NameChange(val input: String) : AddSendAction()

    /**
     * User clicked the file type segmented button.
     */
    data object FileTypeClick : AddSendAction()

    /**
     * User clicked the text type segmented button.
     */
    data object TextTypeClick : AddSendAction()

    /**
     * Value of the send text field updated.
     */
    data class TextChange(val input: String) : AddSendAction()

    /**
     * Value of the password field updated.
     */
    data class PasswordChange(val input: String) : AddSendAction()

    /**
     * Value of the note text field updated.
     */
    data class NoteChange(val input: String) : AddSendAction()

    /**
     * User clicked the choose file button.
     */
    data object ChooseFileClick : AddSendAction()

    /**
     * User toggled the "hide text by default" toggle.
     */
    data class HideByDefaultToggle(val isChecked: Boolean) : AddSendAction()

    /**
     * User incremented the max access count.
     */
    data class MaxAccessCountChange(val value: Int) : AddSendAction()

    /**
     * User toggled the "hide my email" toggle.
     */
    data class HideMyEmailToggle(val isChecked: Boolean) : AddSendAction()

    /**
     * User toggled the "deactivate this send" toggle.
     */
    data class DeactivateThisSendToggle(val isChecked: Boolean) : AddSendAction()
}
