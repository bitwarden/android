package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Trusted Device screen.
 */
@HiltViewModel
class TrustedDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TrustedDeviceState, TrustedDeviceEvent, TrustedDeviceAction>(
    initialState = savedStateHandle[KEY_STATE] ?: TrustedDeviceState,
) {
    override fun handleAction(action: TrustedDeviceAction) {
        when (action) {
            TrustedDeviceAction.BackClick -> handleBackClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(TrustedDeviceEvent.NavigateBack)
    }
}

/**
 * Models the state for the Trusted Device screen.
 */
data object TrustedDeviceState

/**
 * Models events for the Trusted Device screen.
 */
sealed class TrustedDeviceEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : TrustedDeviceEvent()
}

/**
 * Models actions for the Trusted Device screen.
 */
sealed class TrustedDeviceAction {
    /**
     * User clicked back button.
     */
    data object BackClick : TrustedDeviceAction()
}
