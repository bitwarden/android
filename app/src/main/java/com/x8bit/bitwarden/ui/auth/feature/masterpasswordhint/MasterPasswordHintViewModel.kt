package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.PasswordHintResult
import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the master password hint screen.
 */
@HiltViewModel
class MasterPasswordHintViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val networkConnectionManager: NetworkConnectionManager,
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
            is MasterPasswordHintAction.Internal.PasswordHintResultReceive -> {
                handlePasswordHintResult(action)
            }
        }
    }

    private fun handleCloseClick() {
        sendEvent(
            event = MasterPasswordHintEvent.NavigateBack,
        )
    }

    @Suppress("LongMethod")
    private fun handleSubmitClick() {
        val email = stateFlow.value.emailInput

        if (!networkConnectionManager.isNetworkConnected) {
            mutableStateFlow.update {
                it.copy(
                    dialog = MasterPasswordHintState.DialogState.Error(
                        title = R.string.internet_connection_required_title.asText(),
                        message = R.string.internet_connection_required_message.asText(),
                    ),
                )
            }
            return
        }

        if (email.isBlank()) {
            val errorMessage =
                R.string.validation_field_required.asText(R.string.email_address.asText())

            mutableStateFlow.update {
                it.copy(
                    dialog = MasterPasswordHintState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = errorMessage,
                    ),
                )
            }
            return
        }

        if (!email.contains("@")) {
            val errorMessage = R.string.invalid_email.asText()
            mutableStateFlow.update {
                it.copy(
                    dialog = MasterPasswordHintState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = errorMessage,
                    ),
                )
            }
            return
        }

        mutableStateFlow.update {
            it.copy(
                dialog = MasterPasswordHintState.DialogState.Loading(
                    R.string.submitting.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result = authRepository.passwordHintRequest(email)
            sendAction(MasterPasswordHintAction.Internal.PasswordHintResultReceive(result))
        }
    }

    private fun handlePasswordHintResult(
        action: MasterPasswordHintAction.Internal.PasswordHintResultReceive,
    ) {
        when (action.result) {
            is PasswordHintResult.Success -> {
                mutableStateFlow.update {
                    it.copy(dialog = MasterPasswordHintState.DialogState.PasswordHintSent)
                }
            }

            is PasswordHintResult.Error -> {
                val errorMessage = action.result.message?.asText()
                    ?: R.string.generic_error_message.asText()
                mutableStateFlow.update {
                    it.copy(
                        dialog = MasterPasswordHintState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = errorMessage,
                        ),
                    )
                }
            }
        }
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
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Represents an error dialog with the given [message].
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
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

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : MasterPasswordHintAction() {
        /**
         * Indicates that the password hint result was received.
         */
        data class PasswordHintResultReceive(
            val result: PasswordHintResult,
        ) : Internal()
    }
}
