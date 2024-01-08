package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
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
class AddSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AddSendState, AddSendEvent, AddSendAction>(
    initialState = savedStateHandle[KEY_STATE] ?: AddSendState(
        viewState = AddSendState.ViewState.Content(
            common = AddSendState.ViewState.Content.Common(
                name = "",
                maxAccessCount = null,
                passwordInput = "",
                noteInput = "",
                isHideEmailChecked = false,
                isDeactivateChecked = false,
            ),
            selectedType = AddSendState.ViewState.Content.SendType.Text(
                input = "",
                isHideByDefaultChecked = false,
            ),
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
        updateCommonContent {
            it.copy(passwordInput = action.input)
        }
    }

    private fun handleNoteChange(action: AddSendAction.NoteChange) {
        updateCommonContent {
            it.copy(noteInput = action.input)
        }
    }

    private fun handleHideMyEmailToggle(action: AddSendAction.HideMyEmailToggle) {
        updateCommonContent {
            it.copy(isHideEmailChecked = action.isChecked)
        }
    }

    private fun handleDeactivateThisSendToggle(action: AddSendAction.DeactivateThisSendToggle) {
        updateCommonContent {
            it.copy(isDeactivateChecked = action.isChecked)
        }
    }

    private fun handleCloseClick() = sendEvent(AddSendEvent.NavigateBack)

    private fun handleSaveClick() = sendEvent(AddSendEvent.ShowToast("Save Not Implemented"))

    private fun handleNameChange(action: AddSendAction.NameChange) {
        updateCommonContent {
            it.copy(name = action.input)
        }
    }

    private fun handleFileTypeClick() {
        updateContent {
            it.copy(selectedType = AddSendState.ViewState.Content.SendType.File)
        }
    }

    private fun handleTextTypeClick() {
        updateContent {
            it.copy(
                selectedType = AddSendState.ViewState.Content.SendType.Text(
                    input = "",
                    isHideByDefaultChecked = false,
                ),
            )
        }
    }

    private fun handleTextChange(action: AddSendAction.TextChange) {
        updateTextContent {
            it.copy(input = action.input)
        }
    }

    private fun handleHideByDefaultToggle(action: AddSendAction.HideByDefaultToggle) {
        updateTextContent {
            it.copy(isHideByDefaultChecked = action.isChecked)
        }
    }

    private fun handleChooseFileClick() {
        // TODO: allow for file upload: BIT-1085
        sendEvent(AddSendEvent.ShowToast("Not Implemented: File Upload"))
    }

    private fun handleMaxAccessCountChange(action: AddSendAction.MaxAccessCountChange) {
        updateCommonContent { it.copy(maxAccessCount = action.value) }
    }

    private inline fun onContent(
        crossinline block: (AddSendState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? AddSendState.ViewState.Content)?.let(block)
    }

    private inline fun updateContent(
        crossinline block: (
            AddSendState.ViewState.Content,
        ) -> AddSendState.ViewState.Content?,
    ) {
        val currentViewState = state.viewState
        val updatedContent = (currentViewState as? AddSendState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateCommonContent(
        crossinline block: (
            AddSendState.ViewState.Content.Common,
        ) -> AddSendState.ViewState.Content.Common,
    ) {
        updateContent { it.copy(common = block(it.common)) }
    }

    private inline fun updateFileContent(
        crossinline block: (
            AddSendState.ViewState.Content.SendType.File,
        ) -> AddSendState.ViewState.Content.SendType.File,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddSendState.ViewState.Content.SendType.File)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }

    private inline fun updateTextContent(
        crossinline block: (
            AddSendState.ViewState.Content.SendType.Text,
        ) -> AddSendState.ViewState.Content.SendType.Text,
    ) {
        updateContent { currentContent ->
            (currentContent.selectedType as? AddSendState.ViewState.Content.SendType.Text)
                ?.let { currentContent.copy(selectedType = block(it)) }
        }
    }
}

/**
 * Models state for the new send screen.
 */
@Parcelize
data class AddSendState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [AddSendScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [AddSendScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [AddSendScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [AddSendScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val selectedType: SendType,
        ) : ViewState() {

            /**
             * Content data that is common for all item types.
             */
            @Parcelize
            data class Common(
                val name: String,
                // Null here means "not set"
                val maxAccessCount: Int?,
                val passwordInput: String,
                val noteInput: String,
                val isHideEmailChecked: Boolean,
                val isDeactivateChecked: Boolean,
            ) : Parcelable

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
