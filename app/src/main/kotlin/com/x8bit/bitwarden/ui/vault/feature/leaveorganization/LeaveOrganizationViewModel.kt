package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.network.util.isNoConnectionError
import com.bitwarden.network.util.isTimeoutError
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RevokeFromOrganizationResult
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
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
    private val vaultMigrationManager: VaultMigrationManager,
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
            LeaveOrganizationAction.DismissNoNetworkDialog -> handleDismissNoNetworkDialog()
            is LeaveOrganizationAction.Internal.RevokeFromOrganizationResultReceived -> {
                handleRevokeFromOrganizationResultReceived(action)
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
            val result = authRepository.revokeFromOrganization(state.organizationId)
            sendAction(
                LeaveOrganizationAction.Internal.RevokeFromOrganizationResultReceived(result),
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
        clearDialog()
    }

    private fun handleDismissNoNetworkDialog() {
        vaultMigrationManager.clearMigrationState()
        clearDialog()
    }

    private fun handleRevokeFromOrganizationResultReceived(
        action: LeaveOrganizationAction.Internal.RevokeFromOrganizationResultReceived,
    ) {
        when (val result = action.result) {
            is RevokeFromOrganizationResult.Success -> {
                snackbarRelayManager.sendSnackbarData(
                    relay = SnackbarRelay.LEFT_ORGANIZATION,
                    data = BitwardenSnackbarData(
                        message = BitwardenString.you_left_the_organization.asText(),
                    ),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.ItemOrganizationDeclined(
                        organizationId = state.organizationId,
                    ),
                )
                mutableStateFlow.update {
                    it.copy(dialogState = null)
                }
                // Navigation will be handled on RootNavViewModel by migration state change
                vaultMigrationManager.clearMigrationState()
            }

            is RevokeFromOrganizationResult.Error -> {
                val isNetworkError = result.error.isNoConnectionError() ||
                    result.error.isTimeoutError()

                mutableStateFlow.update {
                    it.copy(
                        dialogState = if (isNetworkError) {
                            LeaveOrganizationState.DialogState.NoNetwork(
                                title = BitwardenString.internet_connection_required_title.asText(),
                                message = BitwardenString
                                    .internet_connection_required_message
                                    .asText(),
                                error = result.error,
                            )
                        } else {
                            LeaveOrganizationState.DialogState.Error(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.generic_error_message.asText(),
                                error = result.error,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun clearDialog() {
        mutableStateFlow.update {
            it.copy(
                dialogState = null,
            )
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
            val title: Text,
            val message: Text,
            val error: Throwable? = null,
        ) : DialogState()

        /**
         * No network connection dialog when leave operation fails due to network issues.
         */
        @Parcelize
        data class NoNetwork(
            val title: Text,
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
     * User dismissed the NoNetwork dialog.
     */
    data object DismissNoNetworkDialog : LeaveOrganizationAction()

    /**
     * Internal actions for ViewModel processing.
     */
    sealed class Internal : LeaveOrganizationAction() {
        /**
         * Revoke from organization result received from repository.
         */
        data class RevokeFromOrganizationResultReceived(
            val result: RevokeFromOrganizationResult,
        ) : Internal()
    }
}
