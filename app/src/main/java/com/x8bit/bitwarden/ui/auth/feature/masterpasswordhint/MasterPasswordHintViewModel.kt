package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the master password hint screen.
 */
@HiltViewModel
class MasterPasswordHintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MasterPasswordHintState, MasterPasswordHintEvent, MasterPasswordHintAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: MasterPasswordHintState(
            emailInput = MasterPasswordHintArgs(savedStateHandle).emailAddress,
        ),
) {
    init {
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MasterPasswordHintAction) {
        when (action) {
            MasterPasswordHintAction.CloseClick -> handleCloseClick()
            MasterPasswordHintAction.SubmitClick -> handleSubmitClick()
            is MasterPasswordHintAction.EmailInputChange -> handleEmailInputUpdated(action)
            MasterPasswordHintAction.DismissDialog -> handleDismissDialog()
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = MasterPasswordHintEvent.NavigateBack,
        )
    }

    private fun handleSubmitClick() {
        // TODO (BIT-71): Implement master password hint
    }

    private fun handleEmailInputUpdated(action: MasterPasswordHintAction.EmailInputChange) {
        val email = action.input
        mutableStateFlow.update {
            it.copy(
                emailInput = email,
            )
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }
}

/**
 * Models state of the landing screen.
 */
@Parcelize
data class MasterPasswordHintState(
    val dialog: DialogState? = null,
    val emailInput: String,
) : Parcelable {

    /**
     * Represents the current state of any dialogs on screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a dialog indicating that the password hint was sent.
         */
        @Parcelize
        data object PasswordHintSent : DialogState()

        /**
         * Represents an error dialog with the given [message].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the master password hint screen.
 */
sealed class MasterPasswordHintEvent {

    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : MasterPasswordHintEvent()
}

/**
 * Models actions for the login screen.
 */
sealed class MasterPasswordHintAction {

    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseClick : MasterPasswordHintAction()

    /**
     * Indicates that the top-bar submit button was clicked.
     */
    data object SubmitClick : MasterPasswordHintAction()

    /**
     * Indicates that the input on the email field has changed.
     */
    data class EmailInputChange(val input: String) : MasterPasswordHintAction()

    /**
     * User dismissed the currently displayed dialog.
     */
    data object DismissDialog : MasterPasswordHintAction()
}
