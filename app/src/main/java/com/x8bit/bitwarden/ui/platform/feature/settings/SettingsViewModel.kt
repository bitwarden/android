package com.x8bit.bitwarden.ui.platform.feature.settings

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : BaseViewModel<Unit, SettingsEvent, SettingsAction>(
    initialState = Unit,
) {
    override fun handleAction(action: SettingsAction): Unit = when (action) {
        SettingsAction.AccountSecurityClick -> handleAccountSecurityClick()
    }

    private fun handleAccountSecurityClick() {
        sendEvent(SettingsEvent.NavigateAccountSecurity)
    }
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent {
    /**
     * Navigate to the account security screen.
     */
    data object NavigateAccountSecurity : SettingsEvent()
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {
    /**
     * User clicked account security.
     */
    data object AccountSecurityClick : SettingsAction()
}
