package com.x8bit.bitwarden.ui.feature.landing

import com.x8bit.bitwarden.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Manages application state for the initial landing screen.
 */
@HiltViewModel
class LandingViewModel @Inject constructor() :
    BaseViewModel<LandingState, LandingEvent, LandingAction>(
        initialState = LandingState(
            initialEmailAddress = "",
            isContinueButtonEnabled = true,
            isRememberMeEnabled = false,
        ),
    ) {

    override fun handleAction(action: LandingAction) {
        when (action) {
            LandingAction.ContinueButtonClick -> handleContinueButtonClicked()
            LandingAction.CreateAccountClick -> handleCreateAccountClicked()
            is LandingAction.RememberMeToggle -> handleRememberMeToggled(action)
        }
    }

    private fun handleContinueButtonClicked() {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            isContinueButtonEnabled = false,
        )
    }

    private fun handleCreateAccountClicked() {
        sendEvent(LandingEvent.NavigateToCreateAccount)
    }

    private fun handleRememberMeToggled(action: LandingAction.RememberMeToggle) {
        mutableStateFlow.value = mutableStateFlow.value.copy(
            isRememberMeEnabled = action.isChecked,
        )
    }
}

/**
 * Models state of the landing screen.
 */
data class LandingState(
    val initialEmailAddress: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
)

/**
 * Models events for the landing screen.
 */
sealed class LandingEvent {
    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateToCreateAccount : LandingEvent()
}

/**
 * Models actions for the landing screen.
 */
sealed class LandingAction {
    /**
     * Indicates that the continue button has been clicked and the app should navigate to Login.
     */
    data object ContinueButtonClick : LandingAction()

    /**
     * Indicates that the Create Account text was clicked.
     */
    data object CreateAccountClick : LandingAction()

    /**
     * Indicates that the Remember Me switch has been toggled.
     */
    data class RememberMeToggle(
        val isChecked: Boolean,
    ) : LandingAction()
}
