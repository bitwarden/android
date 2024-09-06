package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import javax.inject.Inject

/**
 * View model for the [ExpiredRegistrationLinkScreen].
 */
class ExpiredRegistrationLinkViewModel @Inject constructor() :
    BaseViewModel<Unit, ExpiredRegistrationLinkEvent, ExpiredRegistrationLinkAction>(
        initialState = Unit,
    ) {
    override fun handleAction(action: ExpiredRegistrationLinkAction) {
        when (action) {
            ExpiredRegistrationLinkAction.CloseClicked -> handleCloseClicked()
            ExpiredRegistrationLinkAction.GoToLoginClicked -> handleGoToLoginClicked()
            ExpiredRegistrationLinkAction.RestartRegistrationClicked -> {
                handleRestartRegistrationClicked()
            }
        }
    }

    private fun handleRestartRegistrationClicked() {
        sendEvent(ExpiredRegistrationLinkEvent.NavigateToStartRegistration)
    }

    private fun handleGoToLoginClicked() {
        sendEvent(ExpiredRegistrationLinkEvent.NavigateToLogin)
    }

    private fun handleCloseClicked() {
        sendEvent(ExpiredRegistrationLinkEvent.NavigateBack)
    }
}

/**
 *  Model the events that can be sent from the [ExpiredRegistrationLinkViewModel].
 */
sealed class ExpiredRegistrationLinkEvent {

    /**
     * Models event to navigate back to the previous screen.
     */
    data object NavigateBack : ExpiredRegistrationLinkEvent()

    /**
     * Models event to navigate to the login screen.
     */
    data object NavigateToLogin : ExpiredRegistrationLinkEvent()

    /**
     * Models event to navigate to the start registration screen.
     */
    data object NavigateToStartRegistration : ExpiredRegistrationLinkEvent()
}

/**
 * Models the actions that can be handled by the [ExpiredRegistrationLinkViewModel].
 */
sealed class ExpiredRegistrationLinkAction {
    /**
     * Indicates the close button was clicked.
     */
    data object CloseClicked : ExpiredRegistrationLinkAction()

    /**
     * Indicated the restart registration button was clicked.
     */
    data object RestartRegistrationClicked : ExpiredRegistrationLinkAction()

    /**
     * Indicated the login button was clicked.
     */
    data object GoToLoginClicked : ExpiredRegistrationLinkAction()
}