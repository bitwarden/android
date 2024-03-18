package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Trusted Device screen.
 */
@HiltViewModel
class TrustedDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    environmentRepository: EnvironmentRepository,
) : BaseViewModel<TrustedDeviceState, TrustedDeviceEvent, TrustedDeviceAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            TrustedDeviceState(
                emailAddress = TrustedDeviceArgs(savedStateHandle).emailAddress,
                environmentLabel = environmentRepository.environment.label,
                isRemembered = false,
                showContinueButton = false,
                showOtherDeviceButton = false,
                showRequestAdminButton = false,
                showMasterPasswordButton = false,
            )
        },
) {
    override fun handleAction(action: TrustedDeviceAction) {
        when (action) {
            TrustedDeviceAction.BackClick -> handleBackClick()
            is TrustedDeviceAction.RememberToggle -> handleRememberToggle(action)
            TrustedDeviceAction.ContinueClick -> handleContinueClick()
            TrustedDeviceAction.ApproveWithAdminClick -> handleApproveWithAdminClick()
            TrustedDeviceAction.ApproveWithDeviceClick -> handleApproveWithDeviceClick()
            TrustedDeviceAction.ApproveWithPasswordClick -> handleApproveWithPasswordClick()
            TrustedDeviceAction.NotYouClick -> handleNotYouClick()
        }
    }

    private fun handleBackClick() {
        sendEvent(TrustedDeviceEvent.NavigateBack)
    }

    private fun handleRememberToggle(action: TrustedDeviceAction.RememberToggle) {
        mutableStateFlow.update { it.copy(isRemembered = action.isRemembered) }
    }

    private fun handleContinueClick() {
        sendEvent(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleApproveWithAdminClick() {
        sendEvent(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleApproveWithDeviceClick() {
        sendEvent(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleApproveWithPasswordClick() {
        sendEvent(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleNotYouClick() {
        sendEvent(TrustedDeviceEvent.ShowToast("Not yet implemented".asText()))
    }
}

/**
 * Models the state for the Trusted Device screen.
 */
@Parcelize
data class TrustedDeviceState(
    val emailAddress: String,
    val environmentLabel: String,
    val isRemembered: Boolean,
    val showContinueButton: Boolean,
    val showOtherDeviceButton: Boolean,
    val showRequestAdminButton: Boolean,
    val showMasterPasswordButton: Boolean,
) : Parcelable

/**
 * Models events for the Trusted Device screen.
 */
sealed class TrustedDeviceEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : TrustedDeviceEvent()

    /**
     * Displays the [message] as a toast.
     */
    data class ShowToast(val message: Text) : TrustedDeviceEvent()
}

/**
 * Models actions for the Trusted Device screen.
 */
sealed class TrustedDeviceAction {
    /**
     * User clicked back button.
     */
    data object BackClick : TrustedDeviceAction()

    /**
     * User toggled the remember device switch.
     */
    data class RememberToggle(
        val isRemembered: Boolean,
    ) : TrustedDeviceAction()

    /**
     * User clicked the "Continue" button.
     */
    data object ContinueClick : TrustedDeviceAction()

    /**
     * User clicked the "Approve with my other device" button.
     */
    data object ApproveWithDeviceClick : TrustedDeviceAction()

    /**
     * User clicked the "Request admin approval" button.
     */
    data object ApproveWithAdminClick : TrustedDeviceAction()

    /**
     * User clicked the "Approve with master password" button.
     */
    data object ApproveWithPasswordClick : TrustedDeviceAction()

    /**
     * Indicates that the "Not you?" text was clicked.
     */
    data object NotYouClick : TrustedDeviceAction()
}
