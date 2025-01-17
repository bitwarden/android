package com.x8bit.bitwarden.ui.vault.feature.importlogins

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.util.toUriOrNull
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarData
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelay
import com.x8bit.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for the [ImportLoginsScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ImportLoginsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vaultRepository: VaultRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
    private val environmentRepository: EnvironmentRepository,
    private val snackbarRelayManager: SnackbarRelayManager,
) :
    BaseViewModel<ImportLoginsState, ImportLoginsEvent, ImportLoginsAction>(
        initialState = run {
            val vaultUrl = environmentRepository.environment.environmentUrlData.webVault
                ?: environmentRepository.environment.environmentUrlData.base
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
                isVaultSyncing = false,
                showBottomSheet = false,
                // attempt to trim the scheme of the vault url
                currentWebVaultUrl = vaultUrl.toUriOrNull()?.host ?: vaultUrl,
                snackbarRelay = ImportLoginsArgs(savedStateHandle).snackBarRelay,
            )
        },
    ) {
    override fun handleAction(action: ImportLoginsAction) {
        when (action) {
            ImportLoginsAction.ConfirmGetStarted -> handleConfirmGetStarted()
            ImportLoginsAction.ConfirmImportLater -> handleConfirmImportLater()
            ImportLoginsAction.DismissDialog -> handleDismissDialog()
            ImportLoginsAction.GetStartedClick -> handleGetStartedClick()
            ImportLoginsAction.ImportLaterClick -> handleImportLaterClick()
            ImportLoginsAction.CloseClick -> handleCloseClick()
            ImportLoginsAction.MoveToInitialContent -> handleMoveToInitialContent()
            ImportLoginsAction.MoveToStepOne -> handleMoveToStepOne()
            ImportLoginsAction.MoveToStepTwo -> handleMoveToStepTwo()
            ImportLoginsAction.MoveToStepThree -> handleMoveToStepThree()
            ImportLoginsAction.MoveToSyncInProgress -> handleMoveToSyncInProgress()
            ImportLoginsAction.HelpClick -> handleHelpClick()
            is ImportLoginsAction.Internal.VaultSyncResultReceived -> {
                handleVaultSyncResultReceived(action)
            }

            ImportLoginsAction.RetryVaultSync -> handleRetryVaultSync()
            ImportLoginsAction.FailedSyncAcknowledged -> handleFailedSyncAcknowledged()
            ImportLoginsAction.SuccessfulSyncAcknowledged -> handleSuccessSyncAcknowledged()
        }
    }

    private fun handleSuccessSyncAcknowledged() {
        mutableStateFlow.update {
            it.copy(
                isVaultSyncing = false,
                showBottomSheet = false,
            )
        }
        // instead of doing inline, this approach to avoid "MaxLineLength" suppression.
        val snackbarData = BitwardenSnackbarData(
            messageHeader = R.string.logins_imported.asText(),
            message = R.string.remember_to_delete_your_imported_password_file_from_your_computer
                .asText(),
        )
        snackbarRelayManager.sendSnackbarData(data = snackbarData, relay = state.snackbarRelay)
        sendEvent(ImportLoginsEvent.NavigateBack)
    }

    private fun handleFailedSyncAcknowledged() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
        sendEvent(ImportLoginsEvent.NavigateBack)
    }

    private fun handleRetryVaultSync() {
        mutableStateFlow.update {
            it.copy(
                isVaultSyncing = true,
                dialogState = null,
            )
        }
        syncVault()
    }

    private fun handleVaultSyncResultReceived(
        action: ImportLoginsAction.Internal.VaultSyncResultReceived,
    ) {
        when (val result = action.result) {
            is SyncVaultDataResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        isVaultSyncing = false,
                        dialogState = ImportLoginsState.DialogState.Error(),
                    )
                }
            }

            is SyncVaultDataResult.Success -> {
                if (result.itemsAvailable) {
                    firstTimeActionManager.storeShowImportLogins(showImportLogins = false)
                    firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = false)
                    mutableStateFlow.update {
                        it.copy(
                            showBottomSheet = true,
                            isVaultSyncing = false,
                        )
                    }
                } else {
                    mutableStateFlow.update {
                        it.copy(
                            isVaultSyncing = false,
                            dialogState = ImportLoginsState.DialogState.Error(
                                message = R.string.no_logins_were_imported.asText(),
                                title = R.string.import_error.asText(),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun handleMoveToSyncInProgress() {
        mutableStateFlow.update { it.copy(isVaultSyncing = true) }
        syncVault()
    }

    private fun handleHelpClick() {
        sendEvent(ImportLoginsEvent.OpenHelpLink)
    }

    private fun handleMoveToStepThree() {
        updateViewState(ImportLoginsState.ViewState.ImportStepThree)
    }

    private fun handleMoveToStepTwo() {
        updateViewState(ImportLoginsState.ViewState.ImportStepTwo)
    }

    private fun handleMoveToStepOne() {
        updateViewState(ImportLoginsState.ViewState.ImportStepOne)
    }

    private fun handleMoveToInitialContent() {
        updateViewState(ImportLoginsState.ViewState.InitialContent)
    }

    private fun handleCloseClick() {
        sendEvent(ImportLoginsEvent.NavigateBack)
    }

    private fun handleImportLaterClick() {
        updateDialogState(ImportLoginsState.DialogState.ImportLater)
    }

    private fun handleGetStartedClick() {
        updateDialogState(ImportLoginsState.DialogState.GetStarted)
    }

    private fun handleDismissDialog() {
        dismissDialog()
    }

    private fun handleConfirmImportLater() {
        dismissDialog()
        firstTimeActionManager.storeShowImportLogins(showImportLogins = false)
        firstTimeActionManager.storeShowImportLoginsSettingsBadge(showBadge = true)
        sendEvent(ImportLoginsEvent.NavigateBack)
    }

    private fun handleConfirmGetStarted() {
        dismissDialog()
        updateViewState(ImportLoginsState.ViewState.ImportStepOne)
    }

    private fun updateViewState(viewState: ImportLoginsState.ViewState) {
        mutableStateFlow.update {
            it.copy(viewState = viewState)
        }
    }

    private fun dismissDialog() {
        updateDialogState(null)
    }

    private fun updateDialogState(dialogState: ImportLoginsState.DialogState?) {
        mutableStateFlow.update {
            it.copy(dialogState = dialogState)
        }
    }

    private fun syncVault() {
        viewModelScope.launch {
            val result = vaultRepository.syncForResult()
            sendAction(ImportLoginsAction.Internal.VaultSyncResultReceived(result))
        }
    }
}

/**
 * Model state for the [ImportLoginsViewModel].
 */
data class ImportLoginsState(
    val dialogState: DialogState?,
    val viewState: ViewState,
    val isVaultSyncing: Boolean,
    val showBottomSheet: Boolean,
    val currentWebVaultUrl: String,
    val snackbarRelay: SnackbarRelay,
) {
    /**
     * Dialog states for the [ImportLoginsViewModel].
     */
    sealed class DialogState {
        abstract val message: Text
        abstract val title: Text?

        /**
         * Import logins later dialog state.
         */
        data object ImportLater : DialogState() {
            override val message: Text =
                R.string.you_can_return_to_complete_this_step_anytime_from_settings.asText()
            override val title: Text = R.string.import_logins_later_dialog_title.asText()
        }

        /**
         * Get started dialog state.
         */
        data object GetStarted : DialogState() {
            override val message: Text =
                R.string.the_following_instructions_will_guide_you_through_importing_logins.asText()
            override val title: Text = R.string.do_you_have_a_computer_available.asText()
        }

        /**
         * Show a dialog with an error message.
         */
        data class Error(
            override val message: Text = R.string.generic_error_message.asText(),
            override val title: Text? = null,
        ) : DialogState()
    }

    /**
     * View states for the [ImportLoginsViewModel].
     */
    sealed class ViewState {
        /**
         * Back action for each view state.
         */
        abstract val backAction: ImportLoginsAction?

        /**
         * Initial content view state.
         */
        data object InitialContent : ViewState() {
            override val backAction: ImportLoginsAction = ImportLoginsAction.CloseClick
        }

        /**
         * Import step one view state.
         */
        data object ImportStepOne : ViewState() {
            override val backAction: ImportLoginsAction = ImportLoginsAction.MoveToInitialContent
        }

        /**
         * Import step two view state.
         */
        data object ImportStepTwo : ViewState() {
            override val backAction: ImportLoginsAction = ImportLoginsAction.MoveToStepOne
        }

        /**
         * Import step three view state.
         */
        data object ImportStepThree : ViewState() {
            override val backAction: ImportLoginsAction = ImportLoginsAction.MoveToStepTwo
        }
    }
}

/**
 * Model events that can be sent from the [ImportLoginsViewModel]
 */
sealed class ImportLoginsEvent {
    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : ImportLoginsEvent()

    /**
     * Open the help link in a browser.
     */
    data object OpenHelpLink : ImportLoginsEvent()
}

/**
 * Model actions that can be handled by the [ImportLoginsViewModel].
 */
sealed class ImportLoginsAction {

    /**
     * User has clicked the "Get Started" button.
     */
    data object GetStartedClick : ImportLoginsAction()

    /**
     * User has clicked the "Import Later" button.
     */
    data object ImportLaterClick : ImportLoginsAction()

    /**
     * User has clicked the "Close" button on the dialog or outside the dialog.
     */
    data object DismissDialog : ImportLoginsAction()

    /**
     * User has confirmed the "Import Later" dialog.
     */
    data object ConfirmImportLater : ImportLoginsAction()

    /**
     * User has confirmed the "Get Started" dialog.
     */
    data object ConfirmGetStarted : ImportLoginsAction()

    /**
     * User has clicked the "Close" icon button.
     */
    data object CloseClick : ImportLoginsAction()

    /**
     * User has clicked the "Help" button.
     */
    data object HelpClick : ImportLoginsAction()

    /**
     * User has performed action which should move to the initial content view state.
     */
    data object MoveToInitialContent : ImportLoginsAction()

    /**
     * User has performed action which should move to the step one view state.
     */
    data object MoveToStepOne : ImportLoginsAction()

    /**
     * User has performed action which should move to the step two view state.
     */
    data object MoveToStepTwo : ImportLoginsAction()

    /**
     * User has performed action which should move to the step three view state.
     */
    data object MoveToStepThree : ImportLoginsAction()

    /**
     * User has performed action which should begin the sync in progress and update the
     * state accordingly.
     */
    data object MoveToSyncInProgress : ImportLoginsAction()

    /**
     * User has chosen to retry vault sync after failure.
     */
    data object RetryVaultSync : ImportLoginsAction()

    /**
     * User has acknowledge failed sync and chose not to retry now.
     */
    data object FailedSyncAcknowledged : ImportLoginsAction()

    /**
     * User has imported logins successfully.
     */
    data object SuccessfulSyncAcknowledged : ImportLoginsAction()

    /**
     * Internal actions to be handled, not triggered by user actions.
     */
    sealed class Internal : ImportLoginsAction() {

        /**
         * Vault sync result received. Process in a synchronous manner.
         */
        data class VaultSyncResultReceived(val result: SyncVaultDataResult) : Internal()
    }
}
