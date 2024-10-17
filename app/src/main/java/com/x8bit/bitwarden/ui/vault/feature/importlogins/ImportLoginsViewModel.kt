package com.x8bit.bitwarden.ui.vault.feature.importlogins

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View model for the [ImportLoginsScreen].
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ImportLoginsViewModel @Inject constructor() :
    BaseViewModel<ImportLoginsState, ImportLoginsEvent, ImportLoginsAction>(
        initialState = ImportLoginsState(
            null,
            viewState = ImportLoginsState.ViewState.InitialContent,
        ),
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
        }
    }

    private fun handleMoveToSyncInProgress() {
        // TODO PM-11186: Implement sync in progress
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
}

/**
 * Model state for the [ImportLoginsViewModel].
 */
data class ImportLoginsState(
    val dialogState: DialogState?,
    val viewState: ViewState,
) {
    /**
     * Dialog states for the [ImportLoginsViewModel].
     */
    sealed class DialogState {
        abstract val message: Text
        abstract val title: Text

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

        /**
         * Sync in progress view state.
         */
        data object SyncInProgress : ViewState() {
            override val backAction: ImportLoginsAction? = null
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
     * User has performed action which should move to the sync in progress view state.
     */
    data object MoveToSyncInProgress : ImportLoginsAction()
}
