package com.bitwarden.testharness.ui.platform.feature.autofill

import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Autofill Placeholder screen.
 */
@HiltViewModel
class AutofillPlaceholderViewModel @Inject constructor() :
    BaseViewModel<Unit, AutofillPlaceholderEvent, AutofillPlaceholderAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: AutofillPlaceholderAction) {
        when (action) {
            AutofillPlaceholderAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(AutofillPlaceholderEvent.NavigateBack)
    }
}

/**
 * Events for Autofill Placeholder screen.
 */
sealed class AutofillPlaceholderEvent {
    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : AutofillPlaceholderEvent()
}

/**
 * Actions for Autofill Placeholder screen.
 */
sealed class AutofillPlaceholderAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AutofillPlaceholderAction()
}
