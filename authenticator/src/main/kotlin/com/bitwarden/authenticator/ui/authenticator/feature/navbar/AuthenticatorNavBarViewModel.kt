package com.bitwarden.authenticator.ui.authenticator.feature.navbar

import com.bitwarden.authenticator.data.auth.repository.AuthRepository
import com.bitwarden.ui.platform.base.BaseViewModel
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
     * The [AuthenticatorNavBarTab] to be associated with the event.
     */
    abstract val tab: AuthenticatorNavBarTab

    /**
     * Navigate to the verification codes screen.
     */
    data object NavigateToVerificationCodes : AuthenticatorNavBarEvent() {
        override val tab: AuthenticatorNavBarTab = AuthenticatorNavBarTab.VerificationCodes
    }

    /**
     * Navigate to the settings screen.
     */
    data object NavigateToSettings : AuthenticatorNavBarEvent() {
        override val tab: AuthenticatorNavBarTab = AuthenticatorNavBarTab.Settings
    }
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
