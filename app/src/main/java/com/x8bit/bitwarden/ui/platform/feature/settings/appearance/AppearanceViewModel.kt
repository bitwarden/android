package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the appearance screen.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor() :
    BaseViewModel<Unit, AppearanceEvent, AppearanceAction>(
        initialState = Unit,
    ) {
    override fun handleAction(action: AppearanceAction): Unit = when (action) {
        AppearanceAction.BackClick -> sendEvent(AppearanceEvent.NavigateBack)
    }
}

/**
 * Models events for the appearance screen.
 */
sealed class AppearanceEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AppearanceEvent()
}

/**
 * Models actions for the appearance screen.
 */
sealed class AppearanceAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AppearanceAction()
}
