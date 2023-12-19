package com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.LocalDataState
import com.x8bit.bitwarden.data.tools.generator.repository.GeneratorRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.tools.feature.generator.passwordhistory.PasswordHistoryState.GeneratedPassword
import com.x8bit.bitwarden.ui.tools.feature.generator.util.toFormattedPattern
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * ViewModel responsible for handling user interactions in the PasswordHistoryScreen.
 */
@HiltViewModel
@Suppress("TooManyFunctions")
class PasswordHistoryViewModel @Inject constructor(
    private val generatorRepository: GeneratorRepository,
) : BaseViewModel<PasswordHistoryState, PasswordHistoryEvent, PasswordHistoryAction>(
    initialState = PasswordHistoryState(PasswordHistoryState.ViewState.Loading),
) {

    init {
        generatorRepository
            .passwordHistoryStateFlow
            .map { PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PasswordHistoryAction) {
        when (action) {
            PasswordHistoryAction.CloseClick -> handleCloseClick()
            is PasswordHistoryAction.PasswordCopyClick -> handleCopyClick(action.password)
            PasswordHistoryAction.PasswordClearClick -> handlePasswordHistoryClearClick()
            is PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive -> {
                handleUpdatePasswordHistoryReceive(action)
            }
        }
    }

    private fun handleUpdatePasswordHistoryReceive(
        action: PasswordHistoryAction.Internal.UpdatePasswordHistoryReceive,
    ) {
        val newState = when (val state = action.state) {
            is LocalDataState.Loading -> PasswordHistoryState.ViewState.Loading

            is LocalDataState.Error -> {
                PasswordHistoryState.ViewState.Error(R.string.an_error_has_occurred.asText())
            }

            is LocalDataState.Loaded -> {
                val passwords = state.data.map { passwordHistoryView ->
                    GeneratedPassword(
                        password = passwordHistoryView.password,
                        date = passwordHistoryView.lastUsedDate.toFormattedPattern(
                            pattern = "MM/dd/yy h:mm a",
                        ),
                    )
                }

                if (passwords.isEmpty()) {
                    PasswordHistoryState.ViewState.Empty
                } else {
                    PasswordHistoryState.ViewState.Content(passwords)
                }
            }
        }

        mutableStateFlow.update {
            it.copy(viewState = newState)
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = PasswordHistoryEvent.NavigateBack,
        )
    }

    private fun handlePasswordHistoryClearClick() {
        viewModelScope.launch {
            generatorRepository.clearPasswordHistory()
        }
    }

    private fun handleCopyClick(password: GeneratedPassword) {
        sendEvent(PasswordHistoryEvent.CopyTextToClipboard(password.password))
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
        data class Error(val message: Text) : ViewState()

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

    /**
     * Copies text to the clipboard.
     */
    data class CopyTextToClipboard(val text: String) : PasswordHistoryEvent()
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

    /**
     * Models actions that the [PasswordHistoryViewModel] itself might send.
     */
    sealed class Internal : PasswordHistoryAction() {

        /**
         * Indicates a password history update is received.
         */
        data class UpdatePasswordHistoryReceive(
            val state: LocalDataState<List<PasswordHistoryView>>,
        ) : Internal()
    }
}
