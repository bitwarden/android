package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.model.AddSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.util.toSendView
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the new send screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class AddSendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepo: AuthRepository,
    private val clock: Clock,
    private val environmentRepo: EnvironmentRepository,
    private val vaultRepo: VaultRepository,
) : BaseViewModel<AddSendState, AddSendEvent, AddSendAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val addSendType = AddSendArgs(savedStateHandle).sendAddType
        AddSendState(
            addSendType = addSendType,
            viewState = when (addSendType) {
                AddSendType.AddItem -> AddSendState.ViewState.Content(
                    common = AddSendState.ViewState.Content.Common(
                        name = "",
                        maxAccessCount = null,
                        passwordInput = "",
                        noteInput = "",
                        isHideEmailChecked = false,
                        isDeactivateChecked = false,
                        deletionDate = ZonedDateTime
                            .now(clock)
                            // We want the default time to be midnight, so we remove all values
                            // beyond days
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusWeeks(1),
                        expirationDate = null,
                    ),
                    selectedType = AddSendState.ViewState.Content.SendType.Text(
                        input = "",
                        isHideByDefaultChecked = false,
                    ),
                )

                is AddSendType.EditItem -> AddSendState.ViewState.Error(
                    "Not yet implemented".asText(),
                )
            },
            dialogState = null,
            isPremiumUser = authRepo.userStateFlow.value?.activeAccount?.isPremium == true,
            baseWebSendUrl = environmentRepo.environment.environmentUrlData.baseWebSendUrl,
        )
    },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        authRepo
            .userStateFlow
            .map { AddSendAction.Internal.UserStateReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AddSendAction): Unit = when (action) {
        is AddSendAction.CloseClick -> handleCloseClick()
        is AddSendAction.DeletionDateChange -> handleDeletionDateChange(action)
        is AddSendAction.ExpirationDateChange -> handleExpirationDateChange(action)
        AddSendAction.DismissDialogClick -> handleDismissDialogClick()
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
        is AddSendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: AddSendAction.Internal): Unit = when (action) {
        is AddSendAction.Internal.CreateSendResultReceive -> handleCreateSendResultReceive(action)
        is AddSendAction.Internal.UserStateReceive -> handleUserStateReceive(action)
    }

    private fun handleCreateSendResultReceive(
        action: AddSendAction.Internal.CreateSendResultReceive,
    ) {
        when (val result = action.result) {
            CreateSendResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is CreateSendResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(AddSendEvent.NavigateBack)
                sendEvent(
                    AddSendEvent.ShowShareSheet(
                        message = result.sendView.toSendUrl(state.baseWebSendUrl),
                    ),
                )
            }
        }
    }

    private fun handleUserStateReceive(action: AddSendAction.Internal.UserStateReceive) {
        mutableStateFlow.update {
            it.copy(isPremiumUser = action.userState?.activeAccount?.isPremium == true)
        }
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

    private fun handleDeletionDateChange(action: AddSendAction.DeletionDateChange) {
        updateCommonContent {
            it.copy(deletionDate = action.deletionDate)
        }
    }

    private fun handleExpirationDateChange(action: AddSendAction.ExpirationDateChange) {
        updateCommonContent {
            it.copy(expirationDate = action.expirationDate)
        }
    }

    private fun handleSaveClick() {
        onContent { content ->
            if (content.common.name.isBlank()) {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AddSendState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.validation_field_required.asText(
                                R.string.name.asText(),
                            ),
                        ),
                    )
                }
                return@onContent
            }
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Loading(
                        message = R.string.saving.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = vaultRepo.createSend(content.toSendView(clock))
                sendAction(AddSendAction.Internal.CreateSendResultReceive(result))
            }
        }
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleNameChange(action: AddSendAction.NameChange) {
        updateCommonContent {
            it.copy(name = action.input)
        }
    }

    private fun handleFileTypeClick() {
        if (!state.isPremiumUser) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = AddSendState.DialogState.Error(
                        title = R.string.send.asText(),
                        message = R.string.send_file_premium_required.asText(),
                    ),
                )
            }
            return
        }
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
        updateCommonContent { common ->
            common.copy(maxAccessCount = action.value.takeUnless { it == 0 })
        }
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
    val addSendType: AddSendType,
    val dialogState: DialogState?,
    val viewState: ViewState,
    val isPremiumUser: Boolean,
    val baseWebSendUrl: String,
) : Parcelable {

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (addSendType) {
            AddSendType.AddItem -> R.string.add_send.asText()
            is AddSendType.EditItem -> R.string.edit_send.asText()
        }

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
                val maxAccessCount: Int?,
                val passwordInput: String,
                val noteInput: String,
                val isHideEmailChecked: Boolean,
                val isDeactivateChecked: Boolean,
                val deletionDate: ZonedDateTime,
                val expirationDate: ZonedDateTime?,
            ) : Parcelable {
                val dateFormatPattern: String get() = "M/d/yyyy"

                val timeFormatPattern: String get() = "hh:mm a"
            }

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
     * Show share sheet.
     */
    data class ShowShareSheet(val message: String) : AddSendEvent()

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
     * User clicked to dismiss the current dialog.
     */
    data object DismissDialogClick : AddSendAction()

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

    /**
     * The user changed the deletion date.
     */
    data class DeletionDateChange(val deletionDate: ZonedDateTime) : AddSendAction()

    /**
     * The user changed the expiration date.
     */
    data class ExpirationDateChange(val expirationDate: ZonedDateTime?) : AddSendAction()

    /**
     * Models actions that the [AddSendViewModel] itself might send.
     */
    sealed class Internal : AddSendAction() {
        /**
         * Indicates what the current [userState] is.
         */
        data class UserStateReceive(val userState: UserState?) : Internal()

        /**
         * Indicates a result for creating a send has been received.
         */
        data class CreateSendResultReceive(val result: CreateSendResult) : Internal()
    }
}
