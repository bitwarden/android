package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import android.os.Parcelable
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.PasswordHistoryState.GeneratedPassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel responsible for handling user interactions in the PasswordHistoryScreen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class PasswordHistoryViewModel @Inject constructor() :
    BaseViewModel<PasswordHistoryState, PasswordHistoryEvent, PasswordHistoryAction>(
        initialState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading),
    ) {

    override fun handleAction(action: PasswordHistoryAction) {
        when (action) {
            PasswordHistoryAction.CloseClick -> handleCloseClick()
            is PasswordHistoryAction.PasswordCopyClick -> handleCopyClick(action.password)
            PasswordHistoryAction.PasswordClearClick -> handlePasswordHistoryClearClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = PasswordHistoryEvent.NavigateBack,
        )
    }

    private fun handlePasswordHistoryClearClick() {
        sendEvent(
            event = PasswordHistoryEvent.ShowToast(
                message = "Not yet implemented.",
            ),
        )
    }

    private fun handleCopyClick(password: GeneratedPassword) {
        sendEvent(
            event = PasswordHistoryEvent.ShowToast(
                message = "Not yet implemented.",
            ),
        )
    }
}

/**
 * Represents the possible states for the password history screen.
 *
 * @property viewState The current view state of the password history screen.
 */
@Parcelize
data class PasswordHistoryState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * Represents the specific view states for the password history screen.
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Loading state for the password history screen.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Error state for the password history screen.
         *
         * @property message The error message to be displayed.
         */
        @Parcelize
        data class Error(val message: String) : ViewState()

        /**
         * Empty state for the password history screen.
         */
        @Parcelize
        data object Empty : ViewState()

        /**
         * Content state for the password history screen.
         *
         * @property passwords A list of generated passwords, each with its creation date.
         */
        @Parcelize
        data class Content(val passwords: List<GeneratedPassword>) : ViewState()
    }

    /**
     * Represents a generated password with its creation date.
     *
     * @property password The generated password.
     * @property date The date when the password was generated.
     */
    @Parcelize
    data class GeneratedPassword(
        val password: String,
        val date: String,
    ) : Parcelable
}

/**
 * Defines the set of events that can occur in the password history screen.
 */
sealed class PasswordHistoryEvent {

    /**
     * Event to show a toast message.
     *
     * @property message The message to be displayed in the toast.
     */
    data class ShowToast(val message: String) : PasswordHistoryEvent()

    /**
     * Event to navigate back to the previous screen.
     */
    data object NavigateBack : PasswordHistoryEvent()
}

/**
 * Represents the set of actions that can be performed in the password history screen.
 */
sealed class PasswordHistoryAction {

    /**
     * Represents the action triggered when a password copy button is clicked.
     *
     * @param password The [GeneratedPassword] to be copied.
     */
    data class PasswordCopyClick(val password: GeneratedPassword) : PasswordHistoryAction()

    /**
     * Action when the clear passwords button is clicked.
     */
    data object PasswordClearClick : PasswordHistoryAction()

    /**
     * Action when the close button is clicked.
     */
    data object CloseClick : PasswordHistoryAction()
}
