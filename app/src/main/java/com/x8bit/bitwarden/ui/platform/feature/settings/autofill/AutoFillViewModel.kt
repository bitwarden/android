package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the auto-fill screen.
 */
@HiltViewModel
class AutoFillViewModel @Inject constructor() : BaseViewModel<Unit, AutoFillEvent, AutoFillAction>(
    initialState = Unit,
) {
    override fun handleAction(action: AutoFillAction): Unit = when (action) {
        AutoFillAction.BackClick -> sendEvent(AutoFillEvent.NavigateBack)
    }
}

/**
 * Models events for the auto-fill screen.
 */
sealed class AutoFillEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AutoFillEvent()
}

/**
 * Models actions for the auto-fill screen.
 */
sealed class AutoFillAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AutoFillAction()
}
