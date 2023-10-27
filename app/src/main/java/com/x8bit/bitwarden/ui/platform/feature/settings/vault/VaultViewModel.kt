package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the vault screen.
 */
@HiltViewModel
class VaultViewModel @Inject constructor() : BaseViewModel<Unit, VaultEvent, VaultAction>(
    initialState = Unit,
) {
    override fun handleAction(action: VaultAction): Unit = when (action) {
        VaultAction.BackClick -> sendEvent(VaultEvent.NavigateBack)
    }
}

/**
 * Models events for the vault screen.
 */
sealed class VaultEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : VaultEvent()
}

/**
 * Models actions for the vault screen.
 */
sealed class VaultAction {
    /**
     * User clicked back button.
     */
    data object BackClick : VaultAction()
}
