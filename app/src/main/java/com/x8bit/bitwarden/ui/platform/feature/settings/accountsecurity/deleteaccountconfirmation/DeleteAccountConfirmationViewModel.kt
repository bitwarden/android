package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.deleteaccountconfirmation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.DeleteAccountResult
import com.x8bit.bitwarden.data.auth.repository.model.RequestOtpResult
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
 * View model for the [DeleteAccountConfirmationScreen].
 */
@HiltViewModel
class DeleteAccountConfirmationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<
    DeleteAccountConfirmationState,
    DeleteAccountConfirmationEvent,
    DeleteAccountConfirmationAction,
    >(
    initialState = savedStateHandle[KEY_STATE] ?: DeleteAccountConfirmationState(
        verificationCode = "",
        dialog = null,
    ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        viewModelScope.launch { authRepository.requestOneTimePasscode() }
    }

    override fun handleAction(action: DeleteAccountConfirmationAction) {
        when (action) {
            is DeleteAccountConfirmationAction.CloseClick -> handleCloseClick()
            is DeleteAccountConfirmationAction.DeleteAccountAcknowledge -> {
                handleDeleteAccountAcknowledge()
            }

            is DeleteAccountConfirmationAction.DismissDialog -> handleDismissDialog()
            is DeleteAccountConfirmationAction.DeleteAccountClick -> handleDeleteAccountClick()

            is DeleteAccountConfirmationAction.ResendCodeClick -> handleResendCodeClick()
            is DeleteAccountConfirmationAction.VerificationCodeTextChange -> {
                handleVerificationCodeTextChange(action)
            }

            is DeleteAccountConfirmationAction.Internal -> handleInternalActions(action)
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

    private fun handleDeleteAccountClick() {
        mutableStateFlow.update {
            it.copy(
                dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
            )
        }
        viewModelScope.launch {
            sendAction(
                DeleteAccountConfirmationAction.Internal.ReceiveDeleteAccountResult(
                    deleteAccountResult = authRepository.deleteAccountWithOneTimePassword(
                        oneTimePassword = state.verificationCode,
                    ),
                ),
            )
        }
    }

    private fun handleResendCodeClick() {
        mutableStateFlow.update {
            it.copy(
                dialog = DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Loading(),
            )
        }
        viewModelScope.launch {
            trySendAction(
                DeleteAccountConfirmationAction.Internal.ReceiveRequestOtpResult(
                    requestOtpResult = authRepository.requestOneTimePasscode(),
                ),
            )
        }
    }

    private fun handleVerificationCodeTextChange(
        action: DeleteAccountConfirmationAction.VerificationCodeTextChange,
    ) {
        mutableStateFlow.update { it.copy(verificationCode = action.verificationCode) }
    }

    private fun handleInternalActions(action: DeleteAccountConfirmationAction.Internal) {
        when (action) {
            is DeleteAccountConfirmationAction.Internal.ReceiveRequestOtpResult -> {
                handleReceiveRequestOtpResult(action)
            }

            is DeleteAccountConfirmationAction.Internal.ReceiveDeleteAccountResult -> {
                handleReceiveDeleteAccountResult(action)
            }
        }
    }

    private fun handleReceiveRequestOtpResult(
        action: DeleteAccountConfirmationAction.Internal.ReceiveRequestOtpResult,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = when (action.requestOtpResult) {
                    is RequestOtpResult.Error -> {
                        DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                            message = R.string.generic_error_message.asText(),
                        )
                    }

                    is RequestOtpResult.Success -> null
                },
            )
        }
    }

    @Suppress("MaxLineLength")
    private fun handleReceiveDeleteAccountResult(
        action: DeleteAccountConfirmationAction.Internal.ReceiveDeleteAccountResult,
    ) {
        mutableStateFlow.update { currentState ->
            currentState.copy(
                dialog = when (val result = action.deleteAccountResult) {
                    is DeleteAccountResult.Error -> {
                        DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.Error(
                            message = result.message?.asText()
                                ?: R.string.generic_error_message.asText(),
                        )
                    }

                    DeleteAccountResult.Success -> {
                        DeleteAccountConfirmationState.DeleteAccountConfirmationDialog.DeleteSuccess()
                    }
                },
            )
        }
    }
}

/**
 * Models state for the [DeleteAccountConfirmationScreen].
 */
@Parcelize
data class DeleteAccountConfirmationState(
    val verificationCode: String,
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
            val message: Text = R.string.your_account_has_been_permanently_deleted.asText(),
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

    /**
     * The user has clicked the delete account button.
     */
    data object DeleteAccountClick : DeleteAccountConfirmationAction()

    /**
     * The user has clicked the resend code button.
     */
    data object ResendCodeClick : DeleteAccountConfirmationAction()

    /**
     * The user has changed the verification code.
     *
     * @param verificationCode The verification code the user has entered.
     */
    data class VerificationCodeTextChange(
        val verificationCode: String,
    ) : DeleteAccountConfirmationAction()

    /**
     * Internal actions for the view model.
     */
    sealed class Internal : DeleteAccountConfirmationAction() {

        /**
         * Indicates that a [RequestOtpResult] has been received.
         */
        data class ReceiveRequestOtpResult(
            val requestOtpResult: RequestOtpResult,
        ) : Internal()

        /**
         * Indicates that a [DeleteAccountResult] has been received.
         */
        data class ReceiveDeleteAccountResult(
            val deleteAccountResult: DeleteAccountResult,
        ) : Internal()
    }
}
