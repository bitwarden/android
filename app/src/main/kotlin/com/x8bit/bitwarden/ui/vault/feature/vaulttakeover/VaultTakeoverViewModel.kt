package com.x8bit.bitwarden.ui.vault.feature.vaulttakeover

import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the [VaultTakeoverScreen].
 */
@HiltViewModel
class VaultTakeoverViewModel @Inject constructor() :
    BaseViewModel<VaultTakeoverState, VaultTakeoverEvent, VaultTakeoverAction>(
        initialState = VaultTakeoverState(
            // TODO: Get from navigation args or repository (PM-28468)
            organizationName = "TODO",
        ),
    ) {

    override fun handleAction(action: VaultTakeoverAction) {
        when (action) {
            VaultTakeoverAction.ContinueClicked -> handleContinueClicked()
            VaultTakeoverAction.DeclineAndLeaveClicked -> handleDeclineAndLeaveClicked()
            VaultTakeoverAction.HelpLinkClicked -> handleHelpLinkClicked()
        }
    }

    private fun handleContinueClicked() {
        sendEvent(VaultTakeoverEvent.NavigateToVault)
    }

    private fun handleDeclineAndLeaveClicked() {
        sendEvent(VaultTakeoverEvent.NavigateToLeaveOrganization)
    }

    private fun handleHelpLinkClicked() {
        // TODO: Update URL when available.
        sendEvent(VaultTakeoverEvent.LaunchUri("TODO_HELP_URL"))
    }
}

/**
 * Models the state for the [VaultTakeoverScreen].
 */
data class VaultTakeoverState(
    val organizationName: String,
)

/**
 * Models the events that can be sent from the [VaultTakeoverViewModel].
 */
sealed class VaultTakeoverEvent {
    /**
     * Navigate to the vault screen after accepting takeover.
     */
    data object NavigateToVault : VaultTakeoverEvent()

    /**
     * Navigate to the leave organization flow after declining.
     */
    data object NavigateToLeaveOrganization : VaultTakeoverEvent()

    /**
     * Launch a URI in the browser or appropriate handler.
     */
    data class LaunchUri(val uri: String) : VaultTakeoverEvent()
}

/**
 * Models the actions that can be handled by the [VaultTakeoverViewModel].
 */
sealed class VaultTakeoverAction {
    /**
     * User clicked the Continue button.
     */
    data object ContinueClicked : VaultTakeoverAction()

    /**
     * User clicked the Decline and Leave button.
     */
    data object DeclineAndLeaveClicked : VaultTakeoverAction()

    /**
     * User clicked the "Why am I seeing this?" help link.
     */
    data object HelpLinkClicked : VaultTakeoverAction()
}
