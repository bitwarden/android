package com.x8bit.bitwarden.ui.vault.feature.importitems

import android.os.Parcelable
import androidx.credentials.providerevents.transfer.CredentialTypes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.cxf.importer.model.ImportCredentialsSelectionResult
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.ImportCredentialsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the [ImportItemsScreen].
 */
@HiltViewModel
class ImportItemsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
) : BaseViewModel<ImportItemsState, ImportItemsEvent, ImportItemsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ImportItemsState(
        viewState = ImportItemsState.ViewState.NotStarted,
    ),
) {

    override fun handleAction(action: ImportItemsAction) {
        when (action) {
            ImportItemsAction.BackClick -> {
                handleBackClick()
            }

            is ImportItemsAction.GetStartedClick -> {
                handleGetStartedClick()
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
        }
    }

    private fun handleReturnToVaultClick() {
        sendEvent(ImportItemsEvent.NavigateToVault)
    }

    private fun handleBackClick() {
        sendEvent(ImportItemsEvent.NavigateBack)
    }

    private fun handleGetStartedClick() {
        mutableStateFlow.update {
            it.copy(viewState = ImportItemsState.ViewState.AwaitingSelection)
        }
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

    private fun handleImportCredentialSelectionReceive(
        action: ImportItemsAction.ImportCredentialSelectionReceive,
    ) {
        when (action.selectionResult) {
            ImportCredentialsSelectionResult.Cancelled -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.import_cancelled.asText(),
                            message = BitwardenString.credential_import_was_cancelled.asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_warning),
                        ),
                    )
                }
            }

            is ImportCredentialsSelectionResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.import_vault_failure.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_warning),
                        ),
                    )
                }
            }

            is ImportCredentialsSelectionResult.Success -> {
                updateImportProgress(BitwardenString.import_items.asText())
                viewModelScope.launch {
                    sendAction(
                        ImportItemsAction.Internal.ImportCredentialsResultReceive(
                            vaultRepository.importCxfPayload(
                                payload = action.selectionResult.response,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun handleImportCredentialsResultReceive(
        action: ImportItemsAction.Internal.ImportCredentialsResultReceive,
    ) {
        updateImportProgress(BitwardenString.uploading_items.asText())
        when (action.result) {
            is ImportCredentialsResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.import_error.asText(),
                            message = BitwardenString
                                .there_was_a_problem_importing_your_items
                                .asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_warning),
                        ),
                    )
                }
            }

            is ImportCredentialsResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.import_success.asText(),
                            message = BitwardenString
                                .your_items_have_been_successfully_imported
                                .asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_plain_checkmark),
                        ),
                    )
                }
            }

            ImportCredentialsResult.NoItems -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.no_items_imported.asText(),
                            message = BitwardenString
                                .no_items_received_from_the_selected_credential_manager
                                .asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_plain_checkmark),
                        ),
                    )
                }
            }

            is ImportCredentialsResult.SyncFailed -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = ImportItemsState.ViewState.Completed(
                            title = BitwardenString.vault_sync_failed.asText(),
                            message = BitwardenString
                                .your_items_have_been_successfully_imported_but_could_not_sync_vault
                                .asText(),
                            iconData = IconData.Local(BitwardenDrawable.ic_warning),
                        ),
                    )
                }
            }
        }
    }

    private fun updateImportProgress(message: Text) {
        mutableStateFlow.update {
            it.copy(
                viewState = ImportItemsState.ViewState.ImportingItems(
                    message = message,
                ),
            )
        }
    }
}

/**
 * State for the [ImportItemsScreen].
 */
@Parcelize
data class ImportItemsState(
    val viewState: ViewState,
) : Parcelable {

    /**
     * View states for the [ImportItemsScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * The import has not yet started.
         */
        data object NotStarted : ViewState()

        /**
         * The import has started and is awaiting selection.
         */
        data object AwaitingSelection : ViewState()

        /**
         * The import is in progress.
         */
        data class ImportingItems(val message: Text) : ViewState()

        /**
         * The import has completed.
         */
        data class Completed(
            val title: Text,
            val message: Text,
            val iconData: IconData,
        ) : ViewState()
    }
}

/**
 * Actions for the [ImportItemsViewModel].
 */
sealed class ImportItemsAction {

    /**
     * User clicked the Get started button.
     */
    data object GetStartedClick : ImportItemsAction()

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
