package com.x8bit.bitwarden.ui.vault.feature.attachments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.vault.feature.attachments.util.toViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel responsible for handling user interactions in the attachments screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val vaultRepo: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<AttachmentsState, AttachmentsEvent, AttachmentsAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: AttachmentsState(
            cipherId = AttachmentsArgs(savedStateHandle).cipherId,
            viewState = AttachmentsState.ViewState.Loading,
            dialogState = null,
        ),
) {
    init {
        vaultRepo
            .getVaultItemStateFlow(state.cipherId)
            .map { AttachmentsAction.Internal.CipherReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AttachmentsAction) {
        when (action) {
            AttachmentsAction.BackClick -> handleBackClick()
            AttachmentsAction.SaveClick -> handleSaveClick()
            AttachmentsAction.DismissDialogClick -> handleDismissDialogClick()
            AttachmentsAction.ChooseFileClick -> handleChooseFileClick()
            is AttachmentsAction.FileChoose -> handleFileChoose(action)
            is AttachmentsAction.DeleteClick -> handleDeleteClick(action)
            is AttachmentsAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleBackClick() {
        sendEvent(AttachmentsEvent.NavigateBack)
    }

    private fun handleSaveClick() {
        sendEvent(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()))
        // TODO: Handle saving the attachments (BIT-522)
    }

    private fun handleDismissDialogClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleChooseFileClick() {
        sendEvent(AttachmentsEvent.ShowChooserSheet)
    }

    private fun handleFileChoose(action: AttachmentsAction.FileChoose) {
        sendEvent(AttachmentsEvent.ShowToast("Not Yet Implemented".asText()))
        // TODO: Handle choosing a file the attachments (BIT-522)
    }

    private fun handleDeleteClick(action: AttachmentsAction.DeleteClick) {
        onContent { content ->
            val cipherView = content.originalCipher ?: return@onContent
            mutableStateFlow.update {
                it.copy(
                    dialogState = AttachmentsState.DialogState.Loading(
                        message = R.string.deleting.asText(),
                    ),
                )
            }
            viewModelScope.launch {
                val result = vaultRepo.deleteCipherAttachment(
                    cipherId = state.cipherId,
                    attachmentId = action.attachmentId,
                    cipherView = cipherView,
                )
                sendAction(AttachmentsAction.Internal.DeleteResultReceive(result))
            }
        }
    }

    private fun handleInternalAction(action: AttachmentsAction.Internal) {
        when (action) {
            is AttachmentsAction.Internal.CipherReceive -> handleCipherReceive(action)
            is AttachmentsAction.Internal.DeleteResultReceive -> handleDeleteResultReceive(action)
        }
    }

    private fun handleCipherReceive(action: AttachmentsAction.Internal.CipherReceive) {
        when (val dataState = action.cipherDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = AttachmentsState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState
                            .data
                            ?.toViewState()
                            ?: AttachmentsState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = AttachmentsState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> mutableStateFlow.update {
                it.copy(
                    viewState = AttachmentsState.ViewState.Error(
                        message = R.string.internet_connection_required_title
                            .asText()
                            .concat("\n".asText())
                            .concat(R.string.internet_connection_required_message.asText()),
                    ),
                )
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState
                            .data
                            ?.toViewState()
                            ?: AttachmentsState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleDeleteResultReceive(action: AttachmentsAction.Internal.DeleteResultReceive) {
        when (action.result) {
            DeleteAttachmentResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = AttachmentsState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteAttachmentResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(AttachmentsEvent.ShowToast(R.string.attachment_deleted.asText()))
            }
        }
    }

    private inline fun onContent(
        crossinline block: (AttachmentsState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? AttachmentsState.ViewState.Content)?.let(block)
    }
}

/**
 * Represents the state for viewing attachments.
 */
@Parcelize
data class AttachmentsState(
    val cipherId: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the specific view states for the [AttachmentsScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [AttachmentsScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [AttachmentsScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [AttachmentsScreen].
         */
        @Parcelize
        data class Content(
            @IgnoredOnParcel
            val originalCipher: CipherView? = null,
            val attachments: List<AttachmentItem>,
        ) : ViewState()
    }

    /**
     * Represents an individual attachment that is already saved to the cipher.
     */
    @Parcelize
    data class AttachmentItem(
        val id: String,
        val title: String,
        val displaySize: String,
    ) : Parcelable

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
 * Represents a set of events related attachments.
 */
sealed class AttachmentsEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : AttachmentsEvent()

    /**
     * Show chooser sheet.
     */
    data object ShowChooserSheet : AttachmentsEvent()

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

    /**
     * User clicked to dismiss a dialog.
     */
    data object DismissDialogClick : AttachmentsAction()

    /**
     * User clicked to select a new attachment file.
     */
    data object ChooseFileClick : AttachmentsAction()

    /**
     * User has chosen the file attachment.
     */
    data class FileChoose(
        val fileData: IntentManager.FileData,
    ) : AttachmentsAction()

    /**
     * User clicked delete an attachment.
     */
    data class DeleteClick(
        val attachmentId: String,
    ) : AttachmentsAction()

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal : AttachmentsAction() {
        /**
         * The cipher data has been received.
         */
        data class CipherReceive(
            val cipherDataState: DataState<CipherView?>,
        ) : Internal()

        /**
         * The result of deleting the attachment.
         */
        data class DeleteResultReceive(
            val result: DeleteAttachmentResult,
        ) : Internal()
    }
}
