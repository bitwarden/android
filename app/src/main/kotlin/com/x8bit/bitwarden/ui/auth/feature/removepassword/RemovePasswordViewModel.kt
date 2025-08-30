package com.x8bit.bitwarden.ui.auth.feature.removepassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LeaveOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.model.RemovePasswordResult
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
        val org = authRepository.userStateFlow.value
            ?.activeAccount
            ?.organizations
            ?.firstOrNull { it.shouldUseKeyConnector }

        RemovePasswordState(
            input = "",
            description = BitwardenString.password_no_longer_required_confirm_domain.asText(),
            labelOrg = BitwardenString.key_connector_organization.asText(),
            orgName = org?.name?.asText(),
            labelDomain = BitwardenString.key_connector_domain.asText(),
            domainName = org?.keyConnectorUrl?.asText(),
            dialogState = null,
            organizationId = org?.id.orNullIfBlank(),
        )
    },
) {
    override fun handleAction(action: RemovePasswordAction) {
        when (action) {
            RemovePasswordAction.ContinueClick -> handleContinueClick()
            is RemovePasswordAction.InputChanged -> handleInputChanged(action)
            RemovePasswordAction.DialogDismiss -> handleDialogDismiss()
            RemovePasswordAction.LeaveOrganizationClick -> handleLeaveOrganizationClick()

            is RemovePasswordAction.ConfirmLeaveOrganizationClick -> {
                handleConfirmLeaveOrganizationResult()
            }

            is RemovePasswordAction.Internal.ReceiveRemovePasswordResult -> {
                handleReceiveRemovePasswordResult(action)
            }

            is RemovePasswordAction.Internal.ReceiveLeaveOrganizationResult -> {
                handleReceiveLeaveOrganizationResult(action)
            }
        }
    }

    private fun handleLeaveOrganizationClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.LeaveConfirmationPrompt(
                    message = BitwardenString.leave_organization_name.asText(state.orgName ?: ""),
                ),
            )
        }
    }

    private fun handleContinueClick() {
        if (state.input.isBlank()) {
            mutableStateFlow.update {
                it.copy(
                    dialogState = RemovePasswordState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.master_password.asText()),
                    ),
                )
            }
            return
        }
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Loading(
                    title = BitwardenString.deleting.asText(),
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
        when (val result = action.result) {
            is RemovePasswordResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RemovePasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            is RemovePasswordResult.WrongPasswordError -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RemovePasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.invalid_master_password.asText(),
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

    private fun handleConfirmLeaveOrganizationResult() {
        mutableStateFlow.update {
            it.copy(
                dialogState = RemovePasswordState.DialogState.Loading(
                    title = BitwardenString.loading.asText(),
                ),
            )
        }

        viewModelScope.launch {
            val result =
                authRepository.leaveOrganization(organizationId = state.organizationId.orEmpty())
            sendAction(
                RemovePasswordAction.Internal.ReceiveLeaveOrganizationResult(
                    result = result,
                ),
            )
        }
    }

    private fun handleReceiveLeaveOrganizationResult(
        action: RemovePasswordAction.Internal.ReceiveLeaveOrganizationResult,
    ) {
        when (val result = action.result) {
            is LeaveOrganizationResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = RemovePasswordState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }

            LeaveOrganizationResult.Success -> {
                mutableStateFlow.update {
                    it.copy(dialogState = null)
                }
                authRepository.logout(
                    reason = LogoutReason.LeftOrganization,
                )
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
    val labelOrg: Text,
    val orgName: Text?,
    val labelDomain: Text,
    val domainName: Text?,
    val dialogState: DialogState?,
    val organizationId: String?,
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
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [title].
         */
        @Parcelize
        data class Loading(val title: Text) : DialogState()

        /**
         * Displays a prompt to confirm leave organization.
         */
        @Parcelize
        data class LeaveConfirmationPrompt(
            val message: Text,
        ) : DialogState()
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
     * Indicates that the user has clicked the leave organization button
     */
    data object LeaveOrganizationClick : RemovePasswordAction()

    /**
     * The user clicked confirm when prompted to leave an organization.
     */
    data object ConfirmLeaveOrganizationClick : RemovePasswordAction()

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

        /**
         * Indicates that a remove password result has been received.
         */
        data class ReceiveLeaveOrganizationResult(
            val result: LeaveOrganizationResult,
        ) : Internal()
    }
}
