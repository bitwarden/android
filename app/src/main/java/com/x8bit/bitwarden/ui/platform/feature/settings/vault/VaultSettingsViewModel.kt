package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the vault screen.
 */
@HiltViewModel
class VaultSettingsViewModel @Inject constructor() :
    BaseViewModel<Unit, VaultSettingsEvent, VaultSettingsAction>(
        initialState = Unit,
    ) {
    override fun handleAction(action: VaultSettingsAction): Unit = when (action) {
        VaultSettingsAction.BackClick -> sendEvent(VaultSettingsEvent.NavigateBack)
    }
}

/**
 * Models events for the vault screen.
 */
sealed class VaultSettingsEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : VaultSettingsEvent()
}

/**
 * Models actions for the vault screen.
 */
sealed class VaultSettingsAction {
    /**
     * User clicked back button.
     */
    data object BackClick : VaultSettingsAction()
}
