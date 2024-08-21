package com.x8bit.bitwarden.ui.auth.feature.removepassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Set Password screen.
 */
@HiltViewModel
class RemovePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<RemovePasswordState, Unit, RemovePasswordAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val orgName = authRepository.userStateFlow.value
            ?.activeAccount
            ?.organizations
            ?.firstOrNull { it.shouldUseKeyConnector }
            ?.name
            .orEmpty()
        RemovePasswordState(
            input = "",
            description = R.string
                .organization_is_using_sso_with_a_self_hosted_key_server
                .asText(orgName),
            dialogState = null,
        )
    },
) {
    override fun handleAction(action: RemovePasswordAction) {
        when (action) {
            RemovePasswordAction.ContinueClick -> handleContinueClick()
            is RemovePasswordAction.InputChanged -> handleInputChanged(action)
            RemovePasswordAction.DialogDismiss -> handleDialogDismiss()
            is RemovePasswordAction.Internal.ReceiveRemovePasswordResult -> {
                handleReceiveRemovePasswordResult(action)
            }
        }
    }

    private fun handleContinueClick() {
        if (state.input.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = RemovePasswordState.DialogState.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required
                            .asText(R.string.master_password.asText()),
                    ),
                )
            }
            return
        }
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Loading(
                    title = R.string.deleting.asText(),
                ),
            )
        }
        viewModelScope.launch {
            val result = authRepository.removePassword(masterPassword = state.input)
            sendAction(RemovePasswordAction.Internal.ReceiveRemovePasswordResult(result))
        }
    }

    private fun handleInputChanged(action: RemovePasswordAction.InputChanged) {
        mutableStateFlow.update { it.copy(input = action.input) }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleReceiveRemovePasswordResult(
        action: RemovePasswordAction.Internal.ReceiveRemovePasswordResult,
    ) {
        when (action.result) {
            RemovePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RemovePasswordState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            RemovePasswordResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                // We do nothing here because state-based navigation will handle it.
            }
        }
    }
}

/**
 * Models state of the Remove Password screen.
 */
@Parcelize
data class RemovePasswordState(
    val input: String,
    val description: Text,
    val dialogState: DialogState?,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents an error dialog with the given [message] and optional [title]. If no title
         * is specified a default will be provided.
         */
        @Parcelize
        data class Error(
            val title: Text? = null,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [title].
         */
        @Parcelize
        data class Loading(val title: Text) : DialogState()
    }
}

/**
 * Models actions for the Remove Password screen.
 */
sealed class RemovePasswordAction {
    /**
     * Indicates that the user has clicked the continue button
     */
    data object ContinueClick : RemovePasswordAction()

    /**
     * The user has modified the input.
     */
    data class InputChanged(
        val input: String,
    ) : RemovePasswordAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DialogDismiss : RemovePasswordAction()

    /**
     * Models actions that the [RemovePasswordViewModel] might send itself.
     */
    sealed class Internal : RemovePasswordAction() {
        /**
         * Indicates that a remove password result has been received.
         */
        data class ReceiveRemovePasswordResult(
            val result: RemovePasswordResult,
        ) : Internal()
    }
}
