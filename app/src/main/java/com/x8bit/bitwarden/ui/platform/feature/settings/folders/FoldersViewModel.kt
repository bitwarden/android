package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
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
) : BaseViewModel<FoldersState, FoldersEvent, FoldersAction>(
    initialState = FoldersState(viewState = FoldersState.ViewState.Loading),
) {
    init {
        vaultRepository
            .foldersStateFlow
            .onEach { sendAction(FoldersAction.Internal.VaultDataReceive(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: FoldersAction): Unit = when (action) {
        is FoldersAction.AddFolderButtonClick -> handleAddFolderButtonClicked()
        is FoldersAction.CloseButtonClick -> handleCloseButtonClicked()
        is FoldersAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
        is FoldersAction.FolderClick -> handleFolderClick(action)
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

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(action: FoldersAction.Internal.VaultDataReceive) {
        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = FoldersState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
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
     * Shows a toast with the given [message].
     */
    data class ShowToast(val message: String) : FoldersEvent()
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
        data class VaultDataReceive(
            val vaultDataState: DataState<List<FolderView>?>,
        ) : Internal()
    }
}
