package com.x8bit.bitwarden.ui.feature.createaccount

import com.x8bit.bitwarden.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Models logic for the create account screen.
 */
@HiltViewModel
class CreateAccountViewModel @Inject constructor() :
    BaseViewModel<CreateAccountState, CreateAccountEvent, CreateAccountAction>(
        initialState = CreateAccountState,
    ) {

    override fun handleAction(action: CreateAccountAction) {
        when (action) {
            is CreateAccountAction.SubmitClick -> handleSubmitClick()
        }
    }

    private fun handleSubmitClick() {
        sendEvent(CreateAccountEvent.ShowToast("TODO: Handle Submit Click"))
    }
}

/**
 * UI state for the create account screen.
 */
data object CreateAccountState

/**
 * Models events for the create account screen.
 */
sealed class CreateAccountEvent {

    /**
     * Placeholder event for showing a toast. Can be removed once there are real events.
     */
    data class ShowToast(val text: String) : CreateAccountEvent()
}

/**
 * Models actions for the create account screen.
 */
sealed class CreateAccountAction {
    /**
     * User clicked submit.
     */
    data object SubmitClick : CreateAccountAction()
}
