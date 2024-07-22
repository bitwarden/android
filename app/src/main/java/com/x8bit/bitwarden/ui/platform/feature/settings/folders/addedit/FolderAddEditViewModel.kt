package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.DateTime
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Handles [FolderAddEditAction],
 * and launches [FolderAddEditEvent] for the [FolderAddEditScreen].
 */
@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class FolderAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<FolderAddEditState, FolderAddEditEvent, FolderAddEditAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val folderAddEditType = FolderAddEditArgs(savedStateHandle).folderAddEditType
            FolderAddEditState(
                folderAddEditType = folderAddEditType,
                viewState = when (folderAddEditType) {
                    is FolderAddEditType.AddItem -> FolderAddEditState.ViewState.Content("")
                    is FolderAddEditType.EditItem -> FolderAddEditState.ViewState.Loading
                },
                dialog = null,
            )
        },
) {
    init {
        state
            .folderAddEditType
            .folderId
            ?.let { folderId ->
                vaultRepository
                    .getVaultFolderStateFlow(folderId)
                    .onEach { sendAction(FolderAddEditAction.Internal.VaultDataReceive(it)) }
                    .launchIn(viewModelScope)
            }
    }

    override fun handleAction(action: FolderAddEditAction) {
        when (action) {
            is FolderAddEditAction.CloseClick -> handleCloseClick()
            is FolderAddEditAction.DeleteClick -> handleDeleteClick()
            is FolderAddEditAction.DismissDialog -> handleDismissDialog()
            is FolderAddEditAction.NameTextChange -> handleNameTextChange(action)
            is FolderAddEditAction.SaveClick -> handleSaveClick()
            is FolderAddEditAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is FolderAddEditAction.Internal.CreateFolderResultReceive ->
                handleCreateFolderResultReceive(action)

            is FolderAddEditAction.Internal.UpdateFolderResultReceive ->
                handleCreateFolderResultReceive(action)

            is FolderAddEditAction.Internal.DeleteFolderResultReceive ->
                handleDeleteResultReceive(action)
        }
    }

    private fun handleCloseClick() {
        sendEvent(FolderAddEditEvent.NavigateBack)
    }

    private fun handleSaveClick() = onContent { content ->
        if (content.folderName.isEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = FolderAddEditState.DialogState.Error(
                        message = R.string.validation_field_required
                            .asText(R.string.name.asText()),
                    ),
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = FolderAddEditState.DialogState.Loading(
                    R.string.saving.asText(),
                ),
            )
        }

        viewModelScope.launch {
            when (val folderAddEditType = state.folderAddEditType) {
                FolderAddEditType.AddItem -> {
                    val result = vaultRepository.createFolder(
                        FolderView(
                            name = content.folderName,
                            id = folderAddEditType.folderId,
                            revisionDate = DateTime.now(),
                        ),
                    )
                    sendAction(FolderAddEditAction.Internal.CreateFolderResultReceive(result))
                }

                is FolderAddEditType.EditItem -> {
                    val result = vaultRepository.updateFolder(
                        folderAddEditType.folderId,
                        FolderView(
                            name = content.folderName,
                            id = folderAddEditType.folderId,
                            revisionDate = DateTime.now(),
                        ),
                    )
                    sendAction(FolderAddEditAction.Internal.UpdateFolderResultReceive(result))
                }
            }
        }
    }

    private fun handleDeleteClick() {
        val folderId = state.folderAddEditType.folderId ?: return

        mutableStateFlow.update {
            it.copy(
                dialog = FolderAddEditState.DialogState.Loading(
                    R.string.deleting.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result =
                vaultRepository.deleteFolder(folderId = folderId)
            sendAction(FolderAddEditAction.Internal.DeleteFolderResultReceive(result))
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleNameTextChange(action: FolderAddEditAction.NameTextChange) {
        mutableStateFlow.update {
            it.copy(
                viewState = FolderAddEditState.ViewState.Content(
                    folderName = action.name,
                ),
            )
        }
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(action: FolderAddEditAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FolderAddEditState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState
                            .data
                            ?.let { folder ->
                                FolderAddEditState.ViewState.Content(
                                    folderName = folder.name,
                                )
                            }
                            ?: FolderAddEditState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }

            is DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = FolderAddEditState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FolderAddEditState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(
                                    " ".asText(),
                                    R.string.internet_connection_required_message.asText(),
                                ),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState
                            .data
                            ?.let { folder ->
                                FolderAddEditState.ViewState.Content(
                                    folderName = folder.name,
                                )
                            }
                            ?: FolderAddEditState.ViewState.Error(
                                message = R.string.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleCreateFolderResultReceive(
        action: FolderAddEditAction.Internal.UpdateFolderResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        when (action.result) {
            is UpdateFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is UpdateFolderResult.Success -> {
                sendEvent(FolderAddEditEvent.ShowToast(R.string.folder_updated.asText()))
                sendEvent(FolderAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleCreateFolderResultReceive(
        action: FolderAddEditAction.Internal.CreateFolderResultReceive,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }

        when (action.result) {
            is CreateFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is CreateFolderResult.Success -> {
                sendEvent(FolderAddEditEvent.ShowToast(R.string.folder_created.asText()))
                sendEvent(FolderAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleDeleteResultReceive(
        action: FolderAddEditAction.Internal.DeleteFolderResultReceive,
    ) {
        when (action.result) {
            DeleteFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteFolderResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                sendEvent(FolderAddEditEvent.ShowToast(R.string.folder_deleted.asText()))
                sendEvent(event = FolderAddEditEvent.NavigateBack)
            }
        }
    }

    private inline fun onContent(
        crossinline block: (FolderAddEditState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? FolderAddEditState.ViewState.Content)?.let(block)
    }
}

/**
 * Represents the state for adding or editing a folder.
 *
 * @property folderAddEditType Indicates whether the VM is in add or edit mode.
 * @property viewState indicates what view state the screen is in.
 * @property dialog the state for the dialogs that can be displayed.
 */
@Parcelize
data class FolderAddEditState(
    val folderAddEditType: FolderAddEditType,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Helper to determine whether we show the overflow menu.
     */
    val shouldShowOverflowMenu: Boolean
        get() = folderAddEditType is FolderAddEditType.EditItem

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (folderAddEditType) {
            FolderAddEditType.AddItem -> R.string.add_folder.asText()
            is FolderAddEditType.EditItem -> R.string.edit_folder.asText()
        }

    /**
     * Represents the specific view states for the [FolderAddEditScreen]
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [FolderAddEditScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Loading state for the [FolderAddEditScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [FolderAddEditScreen].
         */
        @Parcelize
        data class Content(
            val folderName: String,
        ) : ViewState()
    }

    /**
     * Displays a dialog.
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * Displays a loading dialog to the user.
         */
        @Parcelize
        data class Loading(val label: Text) : DialogState()

        /**
         * Displays an error dialog to the user.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Represents a set of events that can be emitted during
 * the process of adding or editing a folder.
 */
sealed class FolderAddEditEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : FolderAddEditEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: Text) : FolderAddEditEvent()
}

/**
 * Represents a set of actions related to the process of adding or editing a folder.
 */
sealed class FolderAddEditAction {

    /**
     * User clicked close.
     */
    data object CloseClick : FolderAddEditAction()

    /**
     * The user has clicked to delete the folder.
     */
    data object DeleteClick : FolderAddEditAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialog : FolderAddEditAction()

    /**
     * Fired when the name text input is changed.
     *
     * @property name The name of the folder.
     */
    data class NameTextChange(val name: String) : FolderAddEditAction()

    /**
     * Represents the action when the save button is clicked.
     */
    data object SaveClick : FolderAddEditAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : FolderAddEditAction() {

        /**
         * The result for deleting a folder has been received.
         */
        data class DeleteFolderResultReceive(val result: DeleteFolderResult) : Internal()

        /**
         * The result for updating a folder has been received.
         */
        data class UpdateFolderResultReceive(val result: UpdateFolderResult) : Internal()

        /**
         * The result for creating a folder has been received.
         */
        data class CreateFolderResultReceive(val result: CreateFolderResult) : Internal()

        /**
         * Indicates that the vault items data has been received.
         */
        data class VaultDataReceive(
            val vaultDataState: DataState<FolderView?>,
        ) : Internal()
    }
}
