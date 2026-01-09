package com.x8bit.bitwarden.ui.platform.feature.settings.folders.addedit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
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
    private val clock: Clock,
    private val vaultRepository: VaultRepository,
    private val relayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<FolderAddEditState, FolderAddEditEvent, FolderAddEditAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val folderAddEditArgs = savedStateHandle.toFolderAddEditArgs()
            FolderAddEditState(
                folderAddEditType = folderAddEditArgs.folderAddEditType,
                viewState = when (folderAddEditArgs.folderAddEditType) {
                    is FolderAddEditType.AddItem -> FolderAddEditState.ViewState.Content("")
                    is FolderAddEditType.EditItem -> FolderAddEditState.ViewState.Loading
                },
                dialog = null,
                parentFolderName = folderAddEditArgs.parentFolderName?.takeUnless { it.isEmpty() },
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
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.name.asText()),
                    ),
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = FolderAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
            )
        }

        viewModelScope.launch {
            when (val folderAddEditType = state.folderAddEditType) {
                FolderAddEditType.AddItem -> {
                    val result = vaultRepository.createFolder(
                        FolderView(
                            name = state
                                .parentFolderName
                                ?.let {
                                    "$it/"
                                }
                                .orEmpty() +
                                content.folderName,
                            id = folderAddEditType.folderId,
                            revisionDate = clock.instant(),
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
                            revisionDate = clock.instant(),
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
                    BitwardenString.deleting.asText(),
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
                            message = BitwardenString.generic_error_message.asText(),
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
                                message = BitwardenString.generic_error_message.asText(),
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
                            message = BitwardenString.internet_connection_required_title
                                .asText()
                                .concat(
                                    " ".asText(),
                                    BitwardenString.internet_connection_required_message.asText(),
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
                                message = BitwardenString.generic_error_message.asText(),
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

        when (val result = action.result) {
            is UpdateFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is UpdateFolderResult.Success -> {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_updated.asText()),
                    relay = SnackbarRelay.FOLDER_UPDATED,
                )
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

        when (val result = action.result) {
            is CreateFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is CreateFolderResult.Success -> {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_created.asText()),
                    relay = SnackbarRelay.FOLDER_CREATED,
                )
                sendEvent(FolderAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleDeleteResultReceive(
        action: FolderAddEditAction.Internal.DeleteFolderResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteFolderResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = FolderAddEditState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            DeleteFolderResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(BitwardenString.folder_deleted.asText()),
                    relay = SnackbarRelay.FOLDER_DELETED,
                )
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
    val parentFolderName: String?,
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
            FolderAddEditType.AddItem -> BitwardenString.add_folder.asText()
            is FolderAddEditType.EditItem -> BitwardenString.edit_folder.asText()
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
            val throwable: Throwable? = null,
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
