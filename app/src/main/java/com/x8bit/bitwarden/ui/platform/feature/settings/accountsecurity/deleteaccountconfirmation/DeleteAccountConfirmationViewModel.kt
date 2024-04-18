package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [DeleteAccountConfirmationScreen].
 */
@HiltViewModel
class DeleteAccountConfirmationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<
    DeleteAccountConfirmationState,
    DeleteAccountConfirmationEvent,
    DeleteAccountConfirmationAction,>(
    initialState = savedStateHandle[KEY_STATE] ?: DeleteAccountConfirmationState(
        dialog = null,
    ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: DeleteAccountConfirmationAction) {
        when (action) {
            DeleteAccountConfirmationAction.CloseClick -> handleCloseClick()
            DeleteAccountConfirmationAction.DeleteAccountAcknowledge -> {
                handleDeleteAccountAcknowledge()
            }

            DeleteAccountConfirmationAction.DismissDialog -> handleDismissDialog()
        }
    }

    private fun handleCloseClick() {
        sendEvent(DeleteAccountConfirmationEvent.NavigateBack)
    }

    private fun handleDeleteAccountAcknowledge() {
        authRepository.clearPendingAccountDeletion()
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }
}

/**
 * Models state for the [DeleteAccountConfirmationScreen].
 */
@Parcelize
data class DeleteAccountConfirmationState(
    val dialog: DeleteAccountConfirmationDialog?,
) : Parcelable {

    /**
     * Displays a dialog.
     */
    sealed class DeleteAccountConfirmationDialog : Parcelable {
        /**
         * Dialog to confirm to the user that the account has been deleted.
         *
         * @param message The message for the dialog.
         */
        @Parcelize
        data class DeleteSuccess(
             val message: Text =
                R.string.your_account_has_been_permanently_deleted.asText(),
        ) : DeleteAccountConfirmationDialog()

        /**
         * Displays the error dialog when deleting an account fails.
         *
         * @param title The title for the dialog.
         * @param message The message for the dialog.
         */
        @Parcelize
        data class Error(
             val title: Text = R.string.an_error_has_occurred.asText(),
             val message: Text,
        ) : DeleteAccountConfirmationDialog()

        /**
         * Displays the loading dialog when deleting an account.
         *
         * @param title The title for the dialog.
         */
        @Parcelize
        data class Loading(
             val title: Text = R.string.loading.asText(),
        ) : DeleteAccountConfirmationDialog()
    }
}

/**
 * Models events for the [DeleteAccountConfirmationScreen].
 */
sealed class DeleteAccountConfirmationEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : DeleteAccountConfirmationEvent()

    /**
     * Displays the [message] in a toast.
     */
    data class ShowToast(
        val message: Text,
    ) : DeleteAccountConfirmationEvent()
}

/**
 * Models actions for the [DeleteAccountConfirmationScreen].
 */
sealed class DeleteAccountConfirmationAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : DeleteAccountConfirmationAction()

    /**
     * The user has dismissed the dialog.
     */
    data object DismissDialog : DeleteAccountConfirmationAction()

    /**
     * The user has acknowledged the account deletion.
     */
    data object DeleteAccountAcknowledge : DeleteAccountConfirmationAction()
}
