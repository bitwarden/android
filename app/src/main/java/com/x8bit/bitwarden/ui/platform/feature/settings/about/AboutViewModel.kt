package com.x8bit.bitwarden.ui.platform.feature.settings.about

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the about screen.
 */
@HiltViewModel
class AboutViewModel @Inject constructor() : BaseViewModel<Unit, AboutEvent, AboutAction>(
    initialState = Unit,
) {
    override fun handleAction(action: AboutAction): Unit = when (action) {
        AboutAction.BackClick -> sendEvent(AboutEvent.NavigateBack)
    }
}

/**
 * Models events for the about screen.
 */
sealed class AboutEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AboutEvent()
}

/**
 * Models actions for the about screen.
 */
sealed class AboutAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AboutAction()
}
