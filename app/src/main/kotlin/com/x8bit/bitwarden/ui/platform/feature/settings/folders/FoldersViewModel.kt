package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderDisplayItem
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Handles [FoldersAction],
 * and launches [FoldersEvent] for the [FoldersScreen].
 */
@HiltViewModel
class FoldersViewModel @Inject constructor(
    vaultRepository: VaultRepository,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<FoldersState, FoldersEvent, FoldersAction>(
    initialState = FoldersState(viewState = FoldersState.ViewState.Loading),
) {
    init {
        vaultRepository
            .foldersStateFlow
            .onEach { sendAction(FoldersAction.Internal.VaultDataReceive(it)) }
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(
                SnackbarRelay.FOLDER_CREATED,
                SnackbarRelay.FOLDER_DELETED,
                SnackbarRelay.FOLDER_UPDATED,
            )
            .map { FoldersAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: FoldersAction): Unit = when (action) {
        is FoldersAction.AddFolderButtonClick -> handleAddFolderButtonClicked()
        is FoldersAction.CloseButtonClick -> handleCloseButtonClicked()
        is FoldersAction.Internal -> handleInternalAction(action)
        is FoldersAction.FolderClick -> handleFolderClick(action)
    }

    private fun handleInternalAction(action: FoldersAction.Internal) {
        when (action) {
            is FoldersAction.Internal.SnackbarDataReceived -> handleSnackbarDataReceived(action)
            is FoldersAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
        }
    }

    private fun handleFolderClick(action: FoldersAction.FolderClick) {
        sendEvent(FoldersEvent.NavigateToEditFolderScreen(action.folderId))
    }

    private fun handleAddFolderButtonClicked() {
        sendEvent(FoldersEvent.NavigateToAddFolderScreen)
    }

    private fun handleCloseButtonClicked() {
        sendEvent(FoldersEvent.NavigateBack)
    }

    private fun handleSnackbarDataReceived(action: FoldersAction.Internal.SnackbarDataReceived) {
        sendEvent(FoldersEvent.ShowSnackbar(action.data))
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(action: FoldersAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FoldersState.ViewState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FoldersState.ViewState.Content(
                            folderList = vaultDataState
                                .data
                                ?.map { folder ->
                                    FolderDisplayItem(
                                        id = folder.id.toString(),
                                        name = folder.name,
                                    )
                                }
                                .orEmpty(),
                        ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = FoldersState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FoldersState.ViewState.Error(
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
                        viewState = FoldersState.ViewState.Content(
                            folderList = vaultDataState
                                .data
                                ?.map { folder ->
                                    FolderDisplayItem(
                                        id = folder.id.toString(),
                                        name = folder.name,
                                    )
                                }
                                .orEmpty(),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Represents the state for the folders screen.
 *
 * @property viewState indicates what view state the screen is in.
 */
@Parcelize
data class FoldersState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the [FoldersScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [FoldersScreen].
         */
        @Parcelize
        data class Error(val message: Text) : ViewState()

        /**
         * Loading state for the [FoldersScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [FoldersScreen].
         */
        @Parcelize
        data class Content(val folderList: List<FolderDisplayItem>) : ViewState()
    }
}

/**
 * Models events for the folders screen.
 */
sealed class FoldersEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : FoldersEvent()

    /**
     * Navigates to the screen to add a folder.
     */
    data object NavigateToAddFolderScreen : FoldersEvent()

    /**
     * Navigates to the screen to edit a folder.
     */
    data class NavigateToEditFolderScreen(val folderId: String) : FoldersEvent()

    /**
     * Show a snackbar.
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : FoldersEvent(), BackgroundEvent
}

/**
 * Models actions for the folders screen.
 */
sealed class FoldersAction {
    /**
     * Indicates that the user clicked the add folder button.
     */
    data object AddFolderButtonClick : FoldersAction()

    /**
     * Indicates that the user clicked a folder.
     */
    data class FolderClick(val folderId: String) : FoldersAction()

    /**
     * Indicates that the user clicked the close button.
     */
    data object CloseButtonClick : FoldersAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : FoldersAction() {
        /**
         * Indicates that the vault folders data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates that the vault folders data has been received.
         */
        data class VaultDataReceive(
            val vaultDataState: DataState<List<FolderView>?>,
        ) : Internal()
    }
}
