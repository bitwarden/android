package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [MigrateToMyItemsScreen].
 */
@HiltViewModel
class MigrateToMyItemsViewModel @Inject constructor(
    private val organizationEventManager: OrganizationEventManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MigrateToMyItemsState, MigrateToMyItemsEvent, MigrateToMyItemsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val args = savedStateHandle.toMigrateToMyItemsArgs()
        MigrateToMyItemsState(
            organizationId = args.organizationId,
            organizationName = args.organizationName,
            dialog = null,
        )
    },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MigrateToMyItemsAction) {
        when (action) {
            MigrateToMyItemsAction.AcceptClicked -> handleAcceptClicked()
            MigrateToMyItemsAction.DeclineAndLeaveClicked -> handleDeclineAndLeaveClicked()
            MigrateToMyItemsAction.HelpLinkClicked -> handleHelpLinkClicked()
            MigrateToMyItemsAction.DismissDialogClicked -> handleDismissDialogClicked()
            is MigrateToMyItemsAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleAcceptClicked() {
        mutableStateFlow.update {
            it.copy(
                dialog = MigrateToMyItemsState.DialogState.Loading(
                    message = BitwardenString.migrating_items_to_x.asText(
                        it.organizationName,
                    ),
                ),
            )
        }

        viewModelScope.launch {
            // TODO: Replace `delay` with actual migration using `state.organizationId` (PM-28444).
            delay(timeMillis = 100L)
            trySendAction(
                MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                    success = true,
                ),
            )
        }
    }

    private fun handleDeclineAndLeaveClicked() {
        sendEvent(MigrateToMyItemsEvent.NavigateToLeaveOrganization)
    }

    private fun handleHelpLinkClicked() {
        sendEvent(
            MigrateToMyItemsEvent.LaunchUri(
                uri = "https://bitwarden.com/help/transfer-ownership/",
            ),
        )
    }

    private fun handleDismissDialogClicked() {
        clearDialog()
    }

    private fun handleInternalAction(action: MigrateToMyItemsAction.Internal) {
        when (action) {
            is MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived -> {
                handleMigrateToMyItemsResultReceived(action)
            }
        }
    }

    private fun handleMigrateToMyItemsResultReceived(
        action: MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived,
    ) {
        if (action.success) {
            organizationEventManager.trackEvent(
                event = OrganizationEvent.ItemOrganizationAccepted,
            )
            clearDialog()
            sendEvent(MigrateToMyItemsEvent.NavigateToVault)
        } else {
            mutableStateFlow.update {
                it.copy(
                    dialog = MigrateToMyItemsState.DialogState.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.failed_to_migrate_items_to_x.asText(
                            it.organizationName,
                        ),
                    ),
                )
            }
        }
    }

    private fun clearDialog() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }
}

/**
 * Models the state for the [MigrateToMyItemsScreen].
 */
@Parcelize
data class MigrateToMyItemsState(
    val organizationId: String,
    val organizationName: String,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Models the dialog state for the [MigrateToMyItemsScreen].
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a loading dialog.
         */
        @Parcelize
        data class Loading(val message: Text) : DialogState()

        /**
         * Displays an error dialog.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
        ) : DialogState()
    }
}

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
     * User clicked the Accept button.
     */
    data object AcceptClicked : MigrateToMyItemsAction()

    /**
     * User clicked the Decline and Leave button.
     */
    data object DeclineAndLeaveClicked : MigrateToMyItemsAction()

    /**
     * User clicked the "Why am I seeing this?" help link.
     */
    data object HelpLinkClicked : MigrateToMyItemsAction()

    /**
     * User dismissed the dialog.
     */
    data object DismissDialogClicked : MigrateToMyItemsAction()

    /**
     * Models internal actions that the [MigrateToMyItemsViewModel] itself may send.
     */
    sealed class Internal : MigrateToMyItemsAction() {

        /**
         * The result of the migration has been received.
         */
        data class MigrateToMyItemsResultReceived(
            // TODO: Replace `success` with actual migration result (PM-28444).
            val success: Boolean,
        ) : Internal()
    }
}
