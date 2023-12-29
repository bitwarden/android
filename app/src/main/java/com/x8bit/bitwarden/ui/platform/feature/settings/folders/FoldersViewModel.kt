package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the folders screen.
 */
@HiltViewModel
class FoldersViewModel @Inject constructor() :
    BaseViewModel<Unit, FoldersEvent, FoldersAction>(
        initialState = Unit,
    ) {
    override fun handleAction(action: FoldersAction): Unit = when (action) {
        FoldersAction.AddFolderButtonClick -> handleAddFolderButtonClicked()
        FoldersAction.CloseButtonClick -> handleCloseButtonClicked()
    }

    private fun handleAddFolderButtonClicked() {
        // TODO BIT-458 implement add folders
        sendEvent(FoldersEvent.ShowToast("Not yet implemented."))
    }

    private fun handleCloseButtonClicked() {
        sendEvent(FoldersEvent.NavigateBack)
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
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : FoldersEvent()
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
     * Indicates that the user clicked the close button.
     */
    data object CloseButtonClick : FoldersAction()
}
