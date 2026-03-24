package com.x8bit.bitwarden.ui.platform.feature.settings.collections.addedit

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.CreateCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCollectionResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCollectionResult
import com.x8bit.bitwarden.ui.platform.feature.settings.collections.model.CollectionAddEditType
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val SLASH_CHAR = "/"

/**
 * Handles [CollectionAddEditAction],
 * and launches [CollectionAddEditEvent] for the [CollectionAddEditScreen].
 */
@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class CollectionAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val relayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<CollectionAddEditState, CollectionAddEditEvent, CollectionAddEditAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val args = savedStateHandle.toCollectionAddEditArgs()
            CollectionAddEditState(
                collectionAddEditType = args.collectionAddEditType,
                viewState = when (args.collectionAddEditType) {
                    is CollectionAddEditType.AddItem -> {
                        CollectionAddEditState.ViewState.Content("")
                    }

                    is CollectionAddEditType.EditItem -> {
                        CollectionAddEditState.ViewState.Loading
                    }
                },
                dialog = null,
            )
        },
) {
    init {
        state
            .collectionAddEditType
            .collectionId
            ?.let { collectionId ->
                vaultRepository
                    .collectionsStateFlow
                    .map { dataState: DataState<List<CollectionView>> ->
                        when (dataState) {
                            is DataState.Error -> DataState.Error<CollectionView?>(
                                data = dataState.data?.find {
                                    it.id.toString() == collectionId
                                },
                                error = dataState.error,
                            )

                            is DataState.Loaded -> DataState.Loaded<CollectionView?>(
                                data = dataState.data.find {
                                    it.id.toString() == collectionId
                                },
                            )

                            is DataState.Loading -> DataState.Loading
                            is DataState.NoNetwork -> DataState.NoNetwork<CollectionView?>(
                                data = dataState.data?.find {
                                    it.id.toString() == collectionId
                                },
                            )

                            is DataState.Pending -> DataState.Pending<CollectionView?>(
                                data = dataState.data.find {
                                    it.id.toString() == collectionId
                                },
                            )
                        }
                    }
                    .onEach {
                        sendAction(CollectionAddEditAction.Internal.VaultDataReceive(it))
                    }
                    .launchIn(viewModelScope)
            }
    }

    override fun handleAction(action: CollectionAddEditAction) {
        when (action) {
            is CollectionAddEditAction.CloseClick -> handleCloseClick()
            is CollectionAddEditAction.DeleteClick -> handleDeleteClick()
            is CollectionAddEditAction.DismissDialog -> handleDismissDialog()
            is CollectionAddEditAction.NameTextChange -> handleNameTextChange(action)
            is CollectionAddEditAction.SaveClick -> handleSaveClick()
            is CollectionAddEditAction.Internal.VaultDataReceive -> {
                handleVaultDataReceive(action)
            }

            is CollectionAddEditAction.Internal.CreateCollectionResultReceive -> {
                handleCreateResultReceive(action)
            }

            is CollectionAddEditAction.Internal.UpdateCollectionResultReceive -> {
                handleUpdateResultReceive(action)
            }

            is CollectionAddEditAction.Internal.DeleteCollectionResultReceive -> {
                handleDeleteResultReceive(action)
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(CollectionAddEditEvent.NavigateBack)
    }

    @Suppress("LongMethod")
    private fun handleSaveClick() = onContent { content ->
        if (content.collectionName.isEmpty()) {
            mutableStateFlow.update {
                it.copy(
                    dialog = CollectionAddEditState.DialogState.Error(
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.name.asText()),
                    ),
                )
            }
            return@onContent
        }

        if (content.collectionName.contains(SLASH_CHAR)) {
            mutableStateFlow.update {
                it.copy(
                    dialog = CollectionAddEditState.DialogState.Error(
                        message = BitwardenString.collection_name_slash_error.asText(),
                    ),
                )
            }
            return@onContent
        }

        mutableStateFlow.update {
            it.copy(
                dialog = CollectionAddEditState.DialogState.Loading(
                    BitwardenString.saving.asText(),
                ),
            )
        }

        val collectionAddEditType = state.collectionAddEditType
        viewModelScope.launch {
            when (collectionAddEditType) {
                is CollectionAddEditType.AddItem -> {
                    val result = vaultRepository.createCollection(
                        organizationId = collectionAddEditType.organizationId,
                        collectionView = CollectionView(
                            id = null,
                            organizationId = collectionAddEditType.organizationId,
                            name = content.collectionName,
                            externalId = null,
                            hidePasswords = false,
                            readOnly = false,
                            manage = true,
                            type = CollectionType.SHARED_COLLECTION,
                        ),
                    )
                    sendAction(
                        CollectionAddEditAction.Internal.CreateCollectionResultReceive(result),
                    )
                }

                is CollectionAddEditType.EditItem -> {
                    val result = vaultRepository.updateCollection(
                        organizationId = collectionAddEditType.organizationId,
                        collectionId = collectionAddEditType.collectionId,
                        collectionView = CollectionView(
                            id = collectionAddEditType.collectionId,
                            organizationId = collectionAddEditType.organizationId,
                            name = content.collectionName,
                            externalId = null,
                            hidePasswords = false,
                            readOnly = false,
                            manage = true,
                            type = CollectionType.SHARED_COLLECTION,
                        ),
                    )
                    sendAction(
                        CollectionAddEditAction.Internal.UpdateCollectionResultReceive(result),
                    )
                }
            }
        }
    }

    private fun handleDeleteClick() {
        val collectionAddEditType = state.collectionAddEditType
        val collectionId = collectionAddEditType.collectionId ?: return

        mutableStateFlow.update {
            it.copy(
                dialog = CollectionAddEditState.DialogState.Loading(
                    BitwardenString.deleting.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result = vaultRepository.deleteCollection(
                organizationId = collectionAddEditType.organizationId,
                collectionId = collectionId,
            )
            sendAction(
                CollectionAddEditAction.Internal.DeleteCollectionResultReceive(result),
            )
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleNameTextChange(action: CollectionAddEditAction.NameTextChange) {
        mutableStateFlow.update {
            it.copy(
                viewState = CollectionAddEditState.ViewState.Content(
                    collectionName = action.name,
                ),
            )
        }
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(
        action: CollectionAddEditAction.Internal.VaultDataReceive,
    ) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = CollectionAddEditState.ViewState.Error(
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
                            ?.let { collection ->
                                CollectionAddEditState.ViewState.Content(
                                    collectionName = collection.name,
                                )
                            }
                            ?: CollectionAddEditState.ViewState.Error(
                                message = BitwardenString.generic_error_message.asText(),
                            ),
                    )
                }
            }

            is DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = CollectionAddEditState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = CollectionAddEditState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState
                            .data
                            ?.let { collection ->
                                CollectionAddEditState.ViewState.Content(
                                    collectionName = collection.name,
                                )
                            }
                            ?: CollectionAddEditState.ViewState.Error(
                                message = BitwardenString.generic_error_message.asText(),
                            ),
                    )
                }
            }
        }
    }

    private fun handleCreateResultReceive(
        action: CollectionAddEditAction.Internal.CreateCollectionResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (val result = action.result) {
            is CreateCollectionResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = CollectionAddEditState.DialogState.Error(
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is CreateCollectionResult.Success -> {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_created.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_CREATED,
                )
                sendEvent(CollectionAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleUpdateResultReceive(
        action: CollectionAddEditAction.Internal.UpdateCollectionResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }
        when (val result = action.result) {
            is UpdateCollectionResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = CollectionAddEditState.DialogState.Error(
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            is UpdateCollectionResult.Success -> {
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_updated.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_UPDATED,
                )
                sendEvent(CollectionAddEditEvent.NavigateBack)
            }
        }
    }

    private fun handleDeleteResultReceive(
        action: CollectionAddEditAction.Internal.DeleteCollectionResultReceive,
    ) {
        when (val result = action.result) {
            is DeleteCollectionResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = CollectionAddEditState.DialogState.Error(
                            message = result
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            throwable = result.error,
                        ),
                    )
                }
            }

            DeleteCollectionResult.Success -> {
                mutableStateFlow.update { it.copy(dialog = null) }
                relayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        BitwardenString.collection_deleted.asText(),
                    ),
                    relay = SnackbarRelay.COLLECTION_DELETED,
                )
                sendEvent(event = CollectionAddEditEvent.NavigateBack)
            }
        }
    }

    private inline fun onContent(
        crossinline block: (CollectionAddEditState.ViewState.Content) -> Unit,
    ) {
        (state.viewState as? CollectionAddEditState.ViewState.Content)?.let(block)
    }
}

/**
 * Represents the state for adding or editing a collection.
 *
 * @property collectionAddEditType Indicates whether the VM is in add or edit mode.
 * @property viewState indicates what view state the screen is in.
 * @property dialog the state for the dialogs that can be displayed.
 */
@Parcelize
data class CollectionAddEditState(
    val collectionAddEditType: CollectionAddEditType,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Helper to determine whether we show the overflow menu.
     */
    val shouldShowOverflowMenu: Boolean
        get() = collectionAddEditType is CollectionAddEditType.EditItem

    /**
     * Helper to determine the screen display name.
     */
    val screenDisplayName: Text
        get() = when (collectionAddEditType) {
            is CollectionAddEditType.AddItem -> BitwardenString.new_collection.asText()
            is CollectionAddEditType.EditItem -> BitwardenString.edit_collection.asText()
        }

    /**
     * Represents the specific view states for the [CollectionAddEditScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [CollectionAddEditScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [CollectionAddEditScreen].
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [CollectionAddEditScreen].
         */
        @Parcelize
        data class Content(val collectionName: String) : ViewState()
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
 * the process of adding or editing a collection.
 */
sealed class CollectionAddEditEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CollectionAddEditEvent()
}

/**
 * Represents a set of actions related to the process of adding or editing a collection.
 */
sealed class CollectionAddEditAction {

    /**
     * User clicked close.
     */
    data object CloseClick : CollectionAddEditAction()

    /**
     * The user has clicked to delete the collection.
     */
    data object DeleteClick : CollectionAddEditAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialog : CollectionAddEditAction()

    /**
     * Fired when the name text input is changed.
     *
     * @property name The name of the collection.
     */
    data class NameTextChange(val name: String) : CollectionAddEditAction()

    /**
     * Represents the action when the save button is clicked.
     */
    data object SaveClick : CollectionAddEditAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : CollectionAddEditAction() {

        /**
         * The result for deleting a collection has been received.
         */
        data class DeleteCollectionResultReceive(
            val result: DeleteCollectionResult,
        ) : Internal()

        /**
         * The result for updating a collection has been received.
         */
        data class UpdateCollectionResultReceive(
            val result: UpdateCollectionResult,
        ) : Internal()

        /**
         * The result for creating a collection has been received.
         */
        data class CreateCollectionResultReceive(
            val result: CreateCollectionResult,
        ) : Internal()

        /**
         * Indicates that the vault collection data has been received.
         */
        data class VaultDataReceive(
            val vaultDataState: DataState<CollectionView?>,
        ) : Internal()
    }
}
