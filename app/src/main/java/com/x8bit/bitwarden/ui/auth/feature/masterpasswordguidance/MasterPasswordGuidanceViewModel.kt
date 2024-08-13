package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the [MasterPasswordGuidanceScreen]
 */
@HiltViewModel
class MasterPasswordGuidanceViewModel @Inject constructor() :
    BaseViewModel<Unit, MasterPasswordGuidanceEvent, MasterPasswordGuidanceAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: MasterPasswordGuidanceAction) {
        when (action) {
            MasterPasswordGuidanceAction.CloseAction -> handleCloseAction()
            MasterPasswordGuidanceAction.TryPasswordGeneratorAction -> {
                handleTryPasswordGeneratorAction()
            }
        }
    }

    private fun handleTryPasswordGeneratorAction() {
        sendEvent(MasterPasswordGuidanceEvent.NavigateToPasswordGenerator)
    }

    private fun handleCloseAction() {
        sendEvent(MasterPasswordGuidanceEvent.NavigateBack)
    }
}

/**
 * Models events for the [MasterPasswordGuidanceScreen]
 */
sealed class MasterPasswordGuidanceEvent {

    /**
     * Navigates back to the previous screen
     */
    data object NavigateBack : MasterPasswordGuidanceEvent()

    /**
     * Navigates to the MasterPasswordGenerationScreen
     */
    data object NavigateToPasswordGenerator : MasterPasswordGuidanceEvent()
}

/**
 * Models user actions on the [MasterPasswordGuidanceScreen]
 */
sealed class MasterPasswordGuidanceAction {

    /**
     * User has clicked the close button
     */
    data object CloseAction : MasterPasswordGuidanceAction()

    /**
     * User has clicked the try generator card
     */
    data object TryPasswordGeneratorAction : MasterPasswordGuidanceAction()
}
