package com.x8bit.bitwarden.ui.platform.feature.settings.other

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the other screen.
 */
@HiltViewModel
class OtherViewModel @Inject constructor() : BaseViewModel<Unit, OtherEvent, OtherAction>(
    initialState = Unit,
) {
    override fun handleAction(action: OtherAction): Unit = when (action) {
        OtherAction.BackClick -> sendEvent(OtherEvent.NavigateBack)
    }
}

/**
 * Models events for the other screen.
 */
sealed class OtherEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : OtherEvent()
}

/**
 * Models actions for the other screen.
 */
sealed class OtherAction {
    /**
     * User clicked back button.
     */
    data object BackClick : OtherAction()
}
