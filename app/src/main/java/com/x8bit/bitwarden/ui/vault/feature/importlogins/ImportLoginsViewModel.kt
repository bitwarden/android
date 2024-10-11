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
@HiltViewModel
class ImportLoginsViewModel @Inject constructor() :
    BaseViewModel<ImportLoginsState, ImportLoginsEvent, ImportLoginsAction>(
        initialState = ImportLoginsState(
            null,
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
        }
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
        // TODO - PM-11182: Move to first step in instructions.
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
}

/**
 * Model events that can be sent from the [ImportLoginsViewModel]
 */
sealed class ImportLoginsEvent {
    /**
     * Navigate back to the previous screen.
     */
    data object NavigateBack : ImportLoginsEvent()
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
}
