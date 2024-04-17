package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the authenticator nav bar screen. Manages bottom tab navigation within the
 * application.
 */
@HiltViewModel
class AuthenticatorNavBarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) :
    BaseViewModel<Unit, AuthenticatorNavBarEvent, AuthenticatorNavBarAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: AuthenticatorNavBarAction) {
        when (action) {
            AuthenticatorNavBarAction.SettingsTabClick -> {
                handleSettingsClick()
            }

            AuthenticatorNavBarAction.VerificationCodesTabClick -> {
                handleVerificationCodesTabClick()
            }

            AuthenticatorNavBarAction.BackStackUpdate -> {
                authRepository.updateLastActiveTime()
            }
        }
    }

    private fun handleSettingsClick() {
        sendEvent(AuthenticatorNavBarEvent.NavigateToSettings)
    }

    private fun handleVerificationCodesTabClick() {
        sendEvent(AuthenticatorNavBarEvent.NavigateToVerificationCodes)
    }
}

/**
 * Models events for the [AuthenticatorNavBarViewModel].
 */
sealed class AuthenticatorNavBarEvent {
    /**
     * Navigate to the verification codes screen.
     */
    data object NavigateToVerificationCodes : AuthenticatorNavBarEvent()

    /**
     * Navigate to the settings screen.
     */
    data object NavigateToSettings : AuthenticatorNavBarEvent()
}

/**
 * Models actions for the bottom tab of.
 */
sealed class AuthenticatorNavBarAction {
    /**
     * User clicked the verification codes tab.
     */
    data object VerificationCodesTabClick : AuthenticatorNavBarAction()

    /**
     * User clicked the settings tab.
     */
    data object SettingsTabClick : AuthenticatorNavBarAction()

    /**
     * Indicates the backstack has changed.
     */
    data object BackStackUpdate : AuthenticatorNavBarAction()
}
