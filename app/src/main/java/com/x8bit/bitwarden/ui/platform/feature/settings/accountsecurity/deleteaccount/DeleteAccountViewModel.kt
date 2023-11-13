package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the account security screen.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor() :
    BaseViewModel<Unit, DeleteAccountEvent, DeleteAccountAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: DeleteAccountAction) {
        when (action) {
            DeleteAccountAction.CancelClick -> handleCancelClick()
            DeleteAccountAction.CloseClick -> handleCloseClick()
            DeleteAccountAction.DeleteAccountClick -> handleDeleteAccountClick()
        }
    }

    private fun handleCancelClick() {
        sendEvent(DeleteAccountEvent.NavigateBack)
    }

    private fun handleCloseClick() {
        sendEvent(DeleteAccountEvent.NavigateBack)
    }

    private fun handleDeleteAccountClick() {
        // TODO: Delete the users account (BIT-1111)
        sendEvent(DeleteAccountEvent.ShowToast("Not yet implemented.".asText()))
    }
}

/**
 * Models events for the delete account screen.
 */
sealed class DeleteAccountEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : DeleteAccountEvent()

    /**
     * Displays the [message] in a toast.
     */
    data class ShowToast(
        val message: Text,
    ) : DeleteAccountEvent()
}

/**
 * Models actions for the delete account screen.
 */
sealed class DeleteAccountAction {
    /**
     * The user has clicked the cancel button.
     */
    data object CancelClick : DeleteAccountAction()

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : DeleteAccountAction()

    /**
     * The user has clicked the delete account button.
     */
    data object DeleteAccountClick : DeleteAccountAction()
}
