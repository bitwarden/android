package com.x8bit.bitwarden.ui.auth.feature.landing

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial landing screen.
 */
@HiltViewModel
class LandingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LandingState, LandingEvent, LandingAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LandingState(
            emailInput = "",
            isContinueButtonEnabled = true,
            isRememberMeEnabled = false,
        ),
) {

    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LandingAction) {
        when (action) {
            is LandingAction.ContinueButtonClick -> handleContinueButtonClicked()
            LandingAction.CreateAccountClick -> handleCreateAccountClicked()
            is LandingAction.RememberMeToggle -> handleRememberMeToggled(action)
            is LandingAction.EmailInputChanged -> handleEmailInputUpdated(action)
        }
    }

    private fun handleEmailInputUpdated(action: LandingAction.EmailInputChanged) {
        mutableStateFlow.update { it.copy(emailInput = action.input) }
    }

    private fun handleContinueButtonClicked() {
        sendEvent(LandingEvent.NavigateToLogin(mutableStateFlow.value.emailInput))
    }

    private fun handleCreateAccountClicked() {
        sendEvent(LandingEvent.NavigateToCreateAccount)
    }

    private fun handleRememberMeToggled(action: LandingAction.RememberMeToggle) {
        mutableStateFlow.update { it.copy(isRememberMeEnabled = action.isChecked) }
    }
}

/**
 * Models state of the landing screen.
 */
@Parcelize
data class LandingState(
    val emailInput: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
) : Parcelable

/**
 * Models events for the landing screen.
 */
sealed class LandingEvent {
    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateToCreateAccount : LandingEvent()

    /**
     * Navigates to the Login screen with the given email address.
     */
    data class NavigateToLogin(
        val emailAddress: String,
    ) : LandingEvent()
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

    /**
     * Indicates that the input on the email field has changed.
     */
    data class EmailInputChanged(
        val input: String,
    ) : LandingAction()
}
