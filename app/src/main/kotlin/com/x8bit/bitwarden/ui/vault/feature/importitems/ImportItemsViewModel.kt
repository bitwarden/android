package com.x8bit.bitwarden.ui.vault.feature.importitems

import android.os.Parcelable
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asPluralsText
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [ImportItemsScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ImportItemsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val snackbarRelayManager: SnackbarRelayManager,
) : BaseViewModel<ImportItemsState, ImportItemsEvent, ImportItemsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ImportItemsState(),
) {

    override fun handleAction(action: ImportItemsAction) {
        when (action) {
            ImportItemsAction.BackClick -> {
                handleBackClick()
            }

            is ImportItemsAction.ImportFromAnotherAppClick -> {
                handleImportFromAnotherAppClick()
            }

            is ImportItemsAction.ImportCredentialSelectionReceive -> {
                handleImportCredentialSelectionReceive(action)
            }

            ImportItemsAction.ReturnToVaultClick -> {
                handleReturnToVaultClick()
            }

            is ImportItemsAction.Internal.ImportCredentialsResultReceive -> {
                handleImportCredentialsResultReceive(action)
            }

            ImportItemsAction.ImportFromComputerClick -> {
                handleImportFromComputerClick()
            }

            ImportItemsAction.DismissDialog -> {
                handleDismissDialog()
            }
        }
    }

    private fun handleReturnToVaultClick() {
        sendEvent(ImportItemsEvent.NavigateToVault)
    }

    private fun handleBackClick() {
        sendEvent(ImportItemsEvent.NavigateBack)
    }

    private fun handleImportFromAnotherAppClick() {
        sendEvent(
            ImportItemsEvent.ShowRegisteredImportSources(
                credentialTypes = listOf(
                    CredentialTypes.BASIC_AUTH,
                    CredentialTypes.PUBLIC_KEY,
                    CredentialTypes.TOTP,
                    CredentialTypes.CREDIT_CARD,
                    CredentialTypes.SSH_KEY,
                    CredentialTypes.ADDRESS,
                ),
            ),
        )
    }

    private fun handleImportFromComputerClick() {
        sendEvent(ImportItemsEvent.NavigateToImportFromComputer)
    }

    private fun handleImportCredentialSelectionReceive(
        action: ImportItemsAction.ImportCredentialSelectionReceive,
    ) {
        when (val result = action.selectionResult) {
            ImportCredentialsSelectionResult.Cancelled -> {
                showGeneralDialog(
                    title = BitwardenString.import_cancelled.asText(),
                    message = BitwardenString.import_was_cancelled_in_the_selected_app.asText(),
                    throwable = null,
                )
            }

            is ImportCredentialsSelectionResult.Failure -> {
                showGeneralDialog(
                    title = BitwardenString.unable_to_import_your_items.asText(),
                    message = BitwardenString.there_was_a_problem_importing_your_items.asText(),
                    throwable = result.error,
                )
            }

            is ImportCredentialsSelectionResult.Success -> {
                updateImportProgress(BitwardenString.saving_items.asText())
                viewModelScope.launch {
                    sendAction(
                        ImportItemsAction.Internal.ImportCredentialsResultReceive(
                            vaultRepository.importCxfPayload(
                                payload = result.response,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun handleDismissDialog() {
        clearDialogs()
    }

    private fun handleImportCredentialsResultReceive(
        action: ImportItemsAction.Internal.ImportCredentialsResultReceive,
    ) {
        when (action.result) {
            is ImportCredentialsResult.Error -> {
                showGeneralDialog(
                    title = BitwardenString.unable_to_import_your_items.asText(),
                    message = BitwardenString.there_was_a_problem_importing_your_items.asText(),
                    throwable = action.result.error,
                )
            }

            is ImportCredentialsResult.Success -> {
                clearDialogs()
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        messageHeader = BitwardenString.import_successful.asText(),
                        message = BitwardenPlurals
                            .x_items_have_been_imported_to_your_vault
                            .asPluralsText(
                                quantity = action.result.itemCount,
                                args = arrayOf(action.result.itemCount),
                            ),
                    ),
                    relay = SnackbarRelay.LOGINS_IMPORTED,
                )
                sendEvent(ImportItemsEvent.NavigateToVault)
            }

            ImportCredentialsResult.NoItems -> {
                showGeneralDialog(
                    title = BitwardenString.no_items_imported.asText(),
                    message = BitwardenString.no_items_received_from_the_selected_app.asText(),
                    throwable = null,
                )
            }

            is ImportCredentialsResult.SyncFailed -> {
                clearDialogs()
                snackbarRelayManager.sendSnackbarData(
                    data = BitwardenSnackbarData(
                        messageHeader = BitwardenString.vault_sync_failed.asText(),
                        message = BitwardenString
                            .your_items_have_been_successfully_imported_but_could_not_sync_vault
                            .asText(),
                        actionLabel = BitwardenString.try_again.asText(),
                    ),
                    relay = SnackbarRelay.VAULT_SYNC_FAILED,
                )
            }
        }
    }

    private fun clearDialogs() {
        mutableStateFlow.update { it.copy(dialog = null) }
    }

    private fun showGeneralDialog(
        title: Text,
        message: Text,
        throwable: Throwable?,
    ) {
        mutableStateFlow.update {
            it.copy(
                dialog = ImportItemsState.DialogState.General(
                    title = title,
                    message = message,
                    throwable = throwable,
                ),
            )
        }
    }

    private fun updateImportProgress(message: Text) {
        mutableStateFlow.update {
            it.copy(
                dialog = ImportItemsState.DialogState.Loading(message = message),
            )
        }
    }
}

/**
 * State for the [ImportItemsScreen].
 */
@Parcelize
data class ImportItemsState(
    val dialog: DialogState? = null,
) : Parcelable {

    /**
     * Dialog state for the [ImportItemsScreen].
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * Show the loading dialog.
         */
        data class Loading(val message: Text) : DialogState()

        /**
         * Show a general dialog with the given title and message.
         */
        data class General(
            val title: Text,
            val message: Text,
            val throwable: Throwable?,
        ) : DialogState()
    }
}

/**
 * Actions for the [ImportItemsViewModel].
 */
sealed class ImportItemsAction {

    /**
     * User clicked the Import from computer option.
     */
    data object ImportFromComputerClick : ImportItemsAction()

    /**
     * User clicked the Import from another app option.
     */
    data object ImportFromAnotherAppClick : ImportItemsAction()

    /**
     * Result of credential selection from the selected credential manager.
     *
     * @property selectionResult The result of the credential selection.
     */
    data class ImportCredentialSelectionReceive(
        val selectionResult: ImportCredentialsSelectionResult,
    ) : ImportItemsAction()

    /**
     * User clicked the Return to vault button.
     */
    data object ReturnToVaultClick : ImportItemsAction()

    /**
     * User clicked the back button.
     */
    data object BackClick : ImportItemsAction()

    /**
     * User dismissed the dialog.
     */
    data object DismissDialog : ImportItemsAction()

    /**
     * Internal actions that the [ImportItemsViewModel] may itself send.
     */
    sealed class Internal : ImportItemsAction() {
        /**
         * Import CXF result received.
         */
        data class ImportCredentialsResultReceive(val result: ImportCredentialsResult) : Internal()
    }
}

/**
 * Events for the [ImportItemsViewModel].
 */
sealed class ImportItemsEvent {

    /**
     * Navigate back.
     */
    data object NavigateBack : ImportItemsEvent()

    /**
     * Navigate to the import from computer screen.
     */
    data object NavigateToImportFromComputer : ImportItemsEvent()

    /**
     * Navigate to the vault.
     */
    data object NavigateToVault : ImportItemsEvent()

    /**
     * Show registered import sources.
     *
     * @property credentialTypes The credential types to request.
     */
    data class ShowRegisteredImportSources(
        val credentialTypes: List<String>,
    ) : ImportItemsEvent(), BackgroundEvent
}
