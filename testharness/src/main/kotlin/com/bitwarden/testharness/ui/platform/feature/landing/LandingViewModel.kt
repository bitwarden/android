package com.bitwarden.testharness.ui.platform.feature.landing

import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the Landing screen.
 *
 * Manages navigation events for the test category selection screen following UDF patterns.
 */
@HiltViewModel
class LandingViewModel @Inject constructor() : BaseViewModel<Unit, LandingEvent, LandingAction>(
    initialState = Unit,
) {
    override fun handleAction(action: LandingAction) {
        when (action) {
            LandingAction.OnAutofillClick -> {
                handleAutofillClicked()
            }

            LandingAction.OnCredentialManagerClick -> {
                handleCredentialManagerClicked()
            }
        }
    }

    private fun handleAutofillClicked() {
        sendEvent(LandingEvent.NavigateToAutofill)
    }

    private fun handleCredentialManagerClicked() {
        sendEvent(LandingEvent.NavigateToCredentialManager)
    }
}

/**
 * Models events emitted by the Landing screen.
 */
sealed class LandingEvent {
    /**
     * Navigates to the Autofill test flow.
     */
    data object NavigateToAutofill : LandingEvent()

    /**
     * Navigates to the Credential Manager test flow.
     */
    data object NavigateToCredentialManager : LandingEvent()
}

/**
 * Models actions for the Landing screen.
 */
sealed class LandingAction {
    /**
     * Indicates the user clicked the Autofill test option.
     */
    data object OnAutofillClick : LandingAction()

    /**
     * Indicates the user clicked the Credential Manager test option.
     */
    data object OnCredentialManagerClick : LandingAction()
}
