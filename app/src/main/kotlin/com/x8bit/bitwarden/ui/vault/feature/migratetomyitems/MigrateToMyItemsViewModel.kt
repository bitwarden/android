package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

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
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.manager.VaultMigrationManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [MigrateToMyItemsScreen].
 */
@HiltViewModel
class MigrateToMyItemsViewModel @Inject constructor(
    private val organizationEventManager: OrganizationEventManager,
    private val vaultMigrationManager: VaultMigrationManager,
    vaultSyncManager: VaultSyncManager,
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<MigrateToMyItemsState, MigrateToMyItemsEvent, MigrateToMyItemsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        // This must be true or we would have never navigated here.
        val migrationData = (vaultMigrationManager.vaultMigrationDataStateFlow.value
            as VaultMigrationData.MigrationRequired)
        MigrateToMyItemsState(
            organizationId = migrationData.organizationId,
            organizationName = migrationData.organizationName,
            dialog = null,
        )
    },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        // We need to ensure we have the most recent data. No need to do anything
        // RootNavViewModel will take care of navigating if needed
        vaultSyncManager.sync(forced = true)
    }

    override fun handleAction(action: MigrateToMyItemsAction) {
        when (action) {
            MigrateToMyItemsAction.AcceptClicked -> handleAcceptClicked()
            MigrateToMyItemsAction.DeclineAndLeaveClicked -> handleDeclineAndLeaveClicked()
            MigrateToMyItemsAction.HelpLinkClicked -> handleHelpLinkClicked()
            MigrateToMyItemsAction.DismissDialogClicked -> handleDismissDialogClicked()
            MigrateToMyItemsAction.NoNetworkDismissDialogClicked ->
                handleNoNetworkDismissDialogClicked()

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
            val userId = authRepository.userStateFlow.value?.activeUserId
            if (userId == null) {
                trySendAction(
                    MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                        result = MigratePersonalVaultResult.Failure(
                            error = NoActiveUserException(),
                        ),
                    ),
                )
                return@launch
            }

            val result = vaultMigrationManager.migratePersonalVault(
                userId = userId,
                organizationId = state.organizationId,
            )

            trySendAction(
                MigrateToMyItemsAction.Internal.MigrateToMyItemsResultReceived(
                    result = result,
                ),
            )
        }
    }

    private fun handleDeclineAndLeaveClicked() {
        sendEvent(
            MigrateToMyItemsEvent.NavigateToLeaveOrganization(
                organizationId = state.organizationId,
                organizationName = state.organizationName,
            ),
        )
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

    private fun handleNoNetworkDismissDialogClicked() {
        vaultMigrationManager.clearMigrationState()
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
        when (val result = action.result) {
            is MigratePersonalVaultResult.Success -> {
                snackbarRelayManager.sendSnackbarData(
                    relay = SnackbarRelay.VAULT_MIGRATED_TO_MY_ITEMS,
                    data = BitwardenSnackbarData(
                        message = BitwardenString.items_transferred.asText(),
                    ),
                )
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.ItemOrganizationAccepted(
                        organizationId = state.organizationId,
                    ),
                )
                clearDialog()
                // Navigation to vault is handled by state-based navigation.
            }

            is MigratePersonalVaultResult.Failure -> {
                Timber.e(result.error, "Failed to migrate personal vault")
                val isNetworkOrTimeoutError = result.error.isNoConnectionError() ||
                    result.error.isTimeoutError()

                mutableStateFlow.update {
                    it.copy(
                        dialog = if (isNetworkOrTimeoutError) {
                            MigrateToMyItemsState.DialogState.NoNetwork(
                                title = BitwardenString.internet_connection_required_title.asText(),
                                message = BitwardenString
                                    .internet_connection_required_message
                                    .asText(),
                                throwable = result.error,
                            )
                        } else {
                            MigrateToMyItemsState.DialogState.Error(
                                title = BitwardenString.an_error_has_occurred.asText(),
                                message = BitwardenString.failed_to_migrate_items_to_x.asText(
                                    it.organizationName,
                                ),
                                throwable = result.error,
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
                dialog = null,
            )
        }
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
            val throwable: Throwable?,
        ) : DialogState()

        /**
         * No network connection dialog when migration operation fails due to network issues.
         */
        @Parcelize
        data class NoNetwork(
            val title: Text,
            val message: Text,
            val throwable: Throwable? = null,
        ) : DialogState()
    }
}

/**
 * Models the events that can be sent from the [MigrateToMyItemsViewModel].
 */
sealed class MigrateToMyItemsEvent {
    /**
     * Navigate to the leave organization flow after declining.
     */
    data class NavigateToLeaveOrganization(
        val organizationId: String,
        val organizationName: String,
    ) : MigrateToMyItemsEvent()

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
     * User dismissed the NoNetwork dialog.
     */
    data object NoNetworkDismissDialogClicked : MigrateToMyItemsAction()

    /**
     * Models internal actions that the [MigrateToMyItemsViewModel] itself may send.
     */
    sealed class Internal : MigrateToMyItemsAction() {

        /**
         * The result of the migration has been received.
         */
        data class MigrateToMyItemsResultReceived(
            val result: MigratePersonalVaultResult,
        ) : Internal()
    }
}
