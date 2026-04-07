package com.bitwarden.testharness.ui.platform.feature.credentialmanager

import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Credential Manager List screen.
 * Manages navigation events to different credential manager test flows.
 */
@HiltViewModel
class CredentialManagerListViewModel @Inject constructor() :
    BaseViewModel<Unit, CredentialManagerListEvent, CredentialManagerListAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: CredentialManagerListAction) {
        when (action) {
            CredentialManagerListAction.GetPasswordClick -> handleGetPasswordClick()
            CredentialManagerListAction.CreatePasswordClick -> handleCreatePasswordClick()
            CredentialManagerListAction.GetPasskeyClick -> handleGetPasskeyClick()
            CredentialManagerListAction.CreatePasskeyClick -> handleCreatePasskeyClick()
            CredentialManagerListAction.GetPasswordOrPasskeyClick -> {
                handleGetPasswordOrPasskeyClick()
            }
            CredentialManagerListAction.BackClick -> handleBackClick()
        }
    }

    private fun handleGetPasswordClick() {
        sendEvent(CredentialManagerListEvent.NavigateToGetPassword)
    }

    private fun handleCreatePasswordClick() {
        sendEvent(CredentialManagerListEvent.NavigateToCreatePassword)
    }

    private fun handleGetPasskeyClick() {
        sendEvent(CredentialManagerListEvent.NavigateToGetPasskey)
    }

    private fun handleCreatePasskeyClick() {
        sendEvent(CredentialManagerListEvent.NavigateToCreatePasskey)
    }

    private fun handleGetPasswordOrPasskeyClick() {
        sendEvent(CredentialManagerListEvent.NavigateToGetPasswordOrPasskey)
    }

    private fun handleBackClick() {
        sendEvent(CredentialManagerListEvent.NavigateBack)
    }
}

/**
 * Events for Credential Manager List screen.
 */
sealed class CredentialManagerListEvent {
    /**
     * Navigate to Get Password test screen.
     */
    data object NavigateToGetPassword : CredentialManagerListEvent()

    /**
     * Navigate to Create Password test screen.
     */
    data object NavigateToCreatePassword : CredentialManagerListEvent()

    /**
     * Navigate to Get Passkey test screen.
     */
    data object NavigateToGetPasskey : CredentialManagerListEvent()

    /**
     * Navigate to Create Passkey test screen.
     */
    data object NavigateToCreatePasskey : CredentialManagerListEvent()

    /**
     * Navigate to Get Password or Passkey test screen.
     */
    data object NavigateToGetPasswordOrPasskey : CredentialManagerListEvent()

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CredentialManagerListEvent()
}

/**
 * Actions for Credential Manager List screen.
 */
sealed class CredentialManagerListAction {
    /**
     * User clicked Get Password button.
     */
    data object GetPasswordClick : CredentialManagerListAction()

    /**
     * User clicked Create Password button.
     */
    data object CreatePasswordClick : CredentialManagerListAction()

    /**
     * User clicked Get Passkey button.
     */
    data object GetPasskeyClick : CredentialManagerListAction()

    /**
     * User clicked Create Passkey button.
     */
    data object CreatePasskeyClick : CredentialManagerListAction()

    /**
     * User clicked Get Password or Passkey button.
     */
    data object GetPasswordOrPasskeyClick : CredentialManagerListAction()

    /**
     * User clicked back button.
     */
    data object BackClick : CredentialManagerListAction()
}
