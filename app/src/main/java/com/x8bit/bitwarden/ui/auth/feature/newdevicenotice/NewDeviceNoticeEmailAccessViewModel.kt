package com.x8bit.bitwarden.ui.auth.feature.newdevicenotice

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessAction.ContinueClick
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessAction.EmailAccessToggle
import com.x8bit.bitwarden.ui.auth.feature.newdevicenotice.NewDeviceNoticeEmailAccessEvent.NavigateToTwoFactorOptions
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the new device notice email access screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class NewDeviceNoticeEmailAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<
    NewDeviceNoticeEmailAccessState,
    NewDeviceNoticeEmailAccessEvent,
    NewDeviceNoticeEmailAccessAction,
    >(
    initialState = savedStateHandle[KEY_STATE]
        ?: NewDeviceNoticeEmailAccessState(
            email = NewDeviceNoticeEmailAccessArgs(savedStateHandle).emailAddress,
            isEmailAccessEnabled = false,
        ),
) {
    override fun handleAction(action: NewDeviceNoticeEmailAccessAction) {
        when (action) {
            ContinueClick -> handleContinueClick()
            is EmailAccessToggle -> handleEmailAccessToggle(action)
        }
    }

    private fun handleContinueClick() {
        // TODO PM-8217: update new device notice status and navigate accordingly
        sendEvent(NavigateToTwoFactorOptions)
    }

    private fun handleEmailAccessToggle(action: EmailAccessToggle) {
        mutableStateFlow.update {
            it.copy(isEmailAccessEnabled = action.newState)
        }
    }
}

/**
 * Models state of the new device notice email access screen.
 */
@Parcelize
data class NewDeviceNoticeEmailAccessState(
    val email: String,
    val isEmailAccessEnabled: Boolean,
) : Parcelable

/**
 * Models events for the new device notice email access screen.
 */
sealed class NewDeviceNoticeEmailAccessEvent {
    /**
     * Navigates to the Two Factor Options screen.
     */
    data object NavigateToTwoFactorOptions : NewDeviceNoticeEmailAccessEvent()
}

/**
 * Models actions for the new device notice email access screen.
 */
sealed class NewDeviceNoticeEmailAccessAction {
    /**
     * User tapped the continue button.
     */
    data object ContinueClick : NewDeviceNoticeEmailAccessAction()

    /**
     * User tapped the email access toggle.
     */
    data class EmailAccessToggle(val newState: Boolean) : NewDeviceNoticeEmailAccessAction()
}
