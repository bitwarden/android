package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the [PreventAccountLockoutScreen].
 */
@HiltViewModel
class PreventAccountLockoutViewModel @Inject constructor() :
    BaseViewModel<Unit, PreventAccountLockoutEvent, PreventAccountLockoutAction>(
        initialState = Unit,
    ) {

    override fun handleAction(action: PreventAccountLockoutAction) {
        when (action) {
            PreventAccountLockoutAction.CloseClickAction -> handleCloseClickAction()
        }
    }

    private fun handleCloseClickAction() = sendEvent(PreventAccountLockoutEvent.NavigateBack)
}

/**
 * Model events to send to the [PreventAccountLockoutScreen].
 */
sealed class PreventAccountLockoutEvent {

    /**
     * Navigates to the previous screen.
     */
    data object NavigateBack : PreventAccountLockoutEvent()
}

/**
 * Model actions to be handled in the [PreventAccountLockoutViewModel].
 */
sealed class PreventAccountLockoutAction {

    /**
     * Close button has been clicked.
     */
    data object CloseClickAction : PreventAccountLockoutAction()
}
