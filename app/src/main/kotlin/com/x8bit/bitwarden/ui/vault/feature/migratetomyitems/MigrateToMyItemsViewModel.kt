package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the [MigrateToMyItemsScreen].
 */
@HiltViewModel
class MigrateToMyItemsViewModel @Inject constructor() :
    BaseViewModel<MigrateToMyItemsState, MigrateToMyItemsEvent, MigrateToMyItemsAction>(
        initialState = MigrateToMyItemsState(
            // TODO: Get from repository or manager (PM-28468).
            organizationName = "TODO",
        ),
    ) {

    override fun handleAction(action: MigrateToMyItemsAction) {
        when (action) {
            MigrateToMyItemsAction.ContinueClicked -> handleContinueClicked()
            MigrateToMyItemsAction.DeclineAndLeaveClicked -> handleDeclineAndLeaveClicked()
            MigrateToMyItemsAction.HelpLinkClicked -> handleHelpLinkClicked()
        }
    }

    private fun handleContinueClicked() {
        sendEvent(MigrateToMyItemsEvent.NavigateToVault)
    }

    private fun handleDeclineAndLeaveClicked() {
        sendEvent(MigrateToMyItemsEvent.NavigateToLeaveOrganization)
    }

    private fun handleHelpLinkClicked() {
        // TODO: Update URL when available.
        sendEvent(MigrateToMyItemsEvent.LaunchUri("TODO_HELP_URL"))
    }
}

/**
 * Models the state for the [MigrateToMyItemsScreen].
 */
data class MigrateToMyItemsState(
    val organizationName: String,
)

/**
 * Models the events that can be sent from the [MigrateToMyItemsViewModel].
 */
sealed class MigrateToMyItemsEvent {
    /**
     * Navigate to the vault screen after accepting migration.
     */
    data object NavigateToVault : MigrateToMyItemsEvent()

    /**
     * Navigate to the leave organization flow after declining.
     */
    data object NavigateToLeaveOrganization : MigrateToMyItemsEvent()

    /**
     * Launch a URI in the browser or appropriate handler.
     */
    data class LaunchUri(val uri: String) : MigrateToMyItemsEvent()
}

/**
 * Models the actions that can be handled by the [MigrateToMyItemsViewModel].
 */
sealed class MigrateToMyItemsAction {
    /**
     * User clicked the Continue button.
     */
    data object ContinueClicked : MigrateToMyItemsAction()

    /**
     * User clicked the Decline and Leave button.
     */
    data object DeclineAndLeaveClicked : MigrateToMyItemsAction()

    /**
     * User clicked the "Why am I seeing this?" help link.
     */
    data object HelpLinkClicked : MigrateToMyItemsAction()
}
