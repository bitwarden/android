package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccount.DeleteAccountState.DeleteAccountDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [DeleteAccountScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<DeleteAccountState, DeleteAccountEvent, DeleteAccountAction>(
    initialState = savedStateHandle[KEY_STATE] ?: DeleteAccountState(
        dialog = null,
        isUnlockWithPasswordEnabled = requireNotNull(authRepository.userStateFlow.value)
            .activeAccount
            .hasMasterPassword,
    ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: DeleteAccountAction) {
        when (action) {
            DeleteAccountAction.CancelClick -> handleCancelClick()
            DeleteAccountAction.CloseClick -> handleCloseClick()
            is DeleteAccountAction.DeleteAccountClick -> handleDeleteAccountClick()
            DeleteAccountAction.AccountDeletionConfirm -> handleAccountDeletionConfirm()
            DeleteAccountAction.DismissDialog -> handleDismissDialog()
            is DeleteAccountAction.Internal -> handleInternalActions(action)

            is DeleteAccountAction.DeleteAccountConfirmDialogClick -> {
                handleDeleteAccountConfirmDialogClick(action)
            }
        }
    }

    private fun handleInternalActions(action: DeleteAccountAction.Internal) {
        when (action) {
            is DeleteAccountAction.Internal.DeleteAccountComplete -> handleDeleteAccountComplete(
                action,
            )

            is DeleteAccountAction.Internal.UpdateDialogState -> updateDialogState(action.dialog)
        }
    }

    private fun handleDeleteAccountClick() {
        sendEvent(DeleteAccountEvent.NavigateToDeleteAccountConfirmationScreen)
    }

    private fun handleCancelClick() {
        sendEvent(DeleteAccountEvent.NavigateBack)
    }

    private fun handleCloseClick() {
        sendEvent(DeleteAccountEvent.NavigateBack)
    }

    private fun handleDeleteAccountConfirmDialogClick(
        action: DeleteAccountAction.DeleteAccountConfirmDialogClick,
    ) {
        updateDialogState(DeleteAccountDialog.Loading)
        viewModelScope.launch {
            val validPasswordResult = authRepository.validatePassword(action.masterPassword)
            if ((validPasswordResult as? ValidatePasswordResult.Success)?.isValid == false) {
                sendAction(
                    DeleteAccountAction.Internal.UpdateDialogState(
                        DeleteAccountDialog.Error(
                            message = R.string.invalid_master_password.asText(),
                        ),
                    ),
                )
            } else {
                val result = authRepository.deleteAccountWithMasterPassword(action.masterPassword)
                sendAction(DeleteAccountAction.Internal.DeleteAccountComplete(result))
            }
        }
    }

    private fun handleAccountDeletionConfirm() {
        authRepository.clearPendingAccountDeletion()
        dismissDialog()
    }

    private fun handleDismissDialog() {
        dismissDialog()
    }

    private fun handleDeleteAccountComplete(
        action: DeleteAccountAction.Internal.DeleteAccountComplete,
    ) {
        when (val result = action.result) {
            DeleteAccountResult.Success -> {
                updateDialogState(DeleteAccountDialog.DeleteSuccess)
            }

            is DeleteAccountResult.Error -> {
                updateDialogState(
                    DeleteAccountDialog.Error(
                        message = result.message?.asText()
                            ?: R.string.generic_error_message.asText(),
                    ),
                )
            }
        }
    }

    private fun updateDialogState(dialog: DeleteAccountDialog?) {
        mutableStateFlow.update {
            it.copy(dialog = dialog)
        }
    }

    private fun dismissDialog() {
        updateDialogState(null)
    }
}

/**
 * Models state for the Delete Account screen.
 *
 * @param dialog The dialog for the [DeleteAccountScreen].
 * @param isUnlockWithPasswordEnabled Whether or not the user is able to unlock the vault with
 * their master password.
 */
@Parcelize
data class DeleteAccountState(
    val dialog: DeleteAccountDialog?,
    val isUnlockWithPasswordEnabled: Boolean,
) : Parcelable {

    /**
     * Displays a dialog.
     */
    sealed class DeleteAccountDialog : Parcelable {
        /**
         * Dialog to confirm to the user that the account has been deleted.
         */
        @Parcelize
        data object DeleteSuccess : DeleteAccountDialog()

        /**
         * Displays the error dialog when deleting an account fails.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : DeleteAccountDialog()

        /**
         * Displays the loading dialog when deleting an account.
         */
        @Parcelize
        data object Loading : DeleteAccountDialog()
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
     * Navigates to the [DeleteAccountConfirmationScreen].
     */
    data object NavigateToDeleteAccountConfirmationScreen : DeleteAccountEvent()

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

    /**
     * The user has clicked the delete account confirmation dialog button.
     *
     * @param masterPassword The master password of the user.
     */
    data class DeleteAccountConfirmDialogClick(
        val masterPassword: String,
    ) : DeleteAccountAction()

    /**
     * The user has confirmed that their account has been deleted.
     */
    data object AccountDeletionConfirm : DeleteAccountAction()

    /**
     * The user has clicked to dismiss the dialog.
     */
    data object DismissDialog : DeleteAccountAction()

    /**
     * Models actions that the [DeleteAccountViewModel] itself might send.
     */
    sealed class Internal : DeleteAccountAction() {
        /**
         * Indicates that the delete account request has completed.
         */
        data class DeleteAccountComplete(
            val result: DeleteAccountResult,
        ) : Internal()

        /**
         * An internal event to update the dialog state utilizing the synchronous action channel.
         */
        data class UpdateDialogState(
            val dialog: DeleteAccountDialog,
        ) : Internal()
    }
}
