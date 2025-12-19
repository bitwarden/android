package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.LeaveOrganizationResult
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * ViewModel for the Leave Organization screen.
 */
@HiltViewModel
class LeaveOrganizationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val organizationEventManager: OrganizationEventManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LeaveOrganizationState, LeaveOrganizationEvent, LeaveOrganizationAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = savedStateHandle.toLeaveOrganizationArgs()
        LeaveOrganizationState(
            organizationId = args.organizationId,
            organizationName = args.organizationName,
            dialogState = null,
        )
    },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LeaveOrganizationAction) {
        when (action) {
            LeaveOrganizationAction.BackClick -> handleBackClick()
            LeaveOrganizationAction.LeaveOrganizationClick -> handleLeaveOrganizationClick()
            LeaveOrganizationAction.HelpLinkClick -> handleHelpLinkClick()
            LeaveOrganizationAction.DismissDialog -> handleDismissDialog()
            is LeaveOrganizationAction.Internal.LeaveOrganizationResultReceived -> {
                handleLeaveOrganizationResultReceived(action)
            }
        }
    }

    private fun handleBackClick() {
        sendEvent(LeaveOrganizationEvent.NavigateBack)
    }

    private fun handleLeaveOrganizationClick() {
        mutableStateFlow.update {
            it.copy(dialogState = LeaveOrganizationState.DialogState.Loading)
        }
        viewModelScope.launch {
            val result = authRepository.leaveOrganization(state.organizationId)
            sendAction(
                LeaveOrganizationAction.Internal.LeaveOrganizationResultReceived(result),
            )
        }
    }

    private fun handleHelpLinkClick() {
        sendEvent(
            LeaveOrganizationEvent.LaunchUri(
                uri = "https://bitwarden.com/help/transfer-ownership/",
            ),
        )
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    private fun handleLeaveOrganizationResultReceived(
        action: LeaveOrganizationAction.Internal.LeaveOrganizationResultReceived,
    ) {
        when (val result = action.result) {
            is LeaveOrganizationResult.Success -> {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.ItemOrganizationDeclined,
                )
                mutableStateFlow.update {
                    it.copy(dialogState = null)
                }
                snackbarRelayManager.sendSnackbarData(
                    relay = SnackbarRelay.LEFT_ORGANIZATION,
                    data = BitwardenSnackbarData(
                        message = BitwardenString.you_left_the_organization.asText(),
                    ),
                )
                sendEvent(LeaveOrganizationEvent.NavigateToVault)
            }

            is LeaveOrganizationResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = LeaveOrganizationState.DialogState.Error(
                            message = BitwardenString.generic_error_message.asText(),
                            error = result.error,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * State for the Leave Organization screen.
 */
@Parcelize
data class LeaveOrganizationState(
    val organizationId: String,
    val organizationName: String,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * Dialog states for transient UI.
     */
    sealed class DialogState : Parcelable {
        /**
         * Loading dialog during leave operation.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * Error dialog when leave operation fails.
         */
        @Parcelize
        data class Error(
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()
    }
}

/**
 * Events for the Leave Organization screen.
 */
sealed class LeaveOrganizationEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : LeaveOrganizationEvent()

    /**
     * Navigate to the Vault screen.
     */
    data object NavigateToVault : LeaveOrganizationEvent()

    /**
     * Launch external URI.
     */
    data class LaunchUri(val uri: String) : LeaveOrganizationEvent()
}

/**
 * Actions for the Leave Organization screen.
 */
sealed class LeaveOrganizationAction {
    /**
     * User clicked the back button.
     */
    data object BackClick : LeaveOrganizationAction()

    /**
     * User clicked the leave organization button.
     */
    data object LeaveOrganizationClick : LeaveOrganizationAction()

    /**
     * User clicked the help link.
     */
    data object HelpLinkClick : LeaveOrganizationAction()

    /**
     * User dismissed a dialog.
     */
    data object DismissDialog : LeaveOrganizationAction()

    /**
     * Internal actions for ViewModel processing.
     */
    sealed class Internal : LeaveOrganizationAction() {
        /**
         * Leave organization result received from repository.
         */
        data class LeaveOrganizationResultReceived(
            val result: LeaveOrganizationResult,
        ) : Internal()
    }
}
