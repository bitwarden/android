package com.x8bit.bitwarden.ui.auth.feature.trusteddevice

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.NewSsoUserResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the Trusted Device screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class TrustedDeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    environmentRepository: EnvironmentRepository,
    private val authRepository: AuthRepository,
) : BaseViewModel<TrustedDeviceState, TrustedDeviceEvent, TrustedDeviceAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val isAuthenticated = authRepository.authStateFlow.value is AuthState.Authenticated
            val account = authRepository.userStateFlow.value?.activeAccount
            val trustedDevice = account?.trustedDevice
            if (trustedDevice == null || !isAuthenticated) authRepository.logout()
            TrustedDeviceState(
                dialogState = null,
                emailAddress = account?.email.orEmpty(),
                environmentLabel = environmentRepository.environment.label,
                isRemembered = true,
                showContinueButton = trustedDevice
                    ?.let { !it.hasAdminApproval && !account.hasMasterPassword }
                    ?: false,
                showOtherDeviceButton = trustedDevice?.hasLoginApprovingDevice ?: false,
                showRequestAdminButton = trustedDevice?.hasAdminApproval ?: false,
                showMasterPasswordButton = account?.hasMasterPassword ?: false,
            )
        },
) {
    override fun handleAction(action: TrustedDeviceAction) {
        when (action) {
            TrustedDeviceAction.BackClick -> handleBackClick()
            TrustedDeviceAction.DismissDialog -> handleDismissDialog()
            is TrustedDeviceAction.RememberToggle -> handleRememberToggle(action)
            TrustedDeviceAction.ContinueClick -> handleContinueClick()
            TrustedDeviceAction.ApproveWithAdminClick -> handleApproveWithAdminClick()
            TrustedDeviceAction.ApproveWithDeviceClick -> handleApproveWithDeviceClick()
            TrustedDeviceAction.ApproveWithPasswordClick -> handleApproveWithPasswordClick()
            TrustedDeviceAction.NotYouClick -> handleNotYouClick()
            is TrustedDeviceAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: TrustedDeviceAction.Internal) {
        when (action) {
            is TrustedDeviceAction.Internal.ReceiveNewSsoUserResult -> {
                handleReceiveNewSsoUserResult(action)
            }
        }
    }

    private fun handleReceiveNewSsoUserResult(
        action: TrustedDeviceAction.Internal.ReceiveNewSsoUserResult,
    ) {
        when (action.result) {
            NewSsoUserResult.Failure -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = TrustedDeviceState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            NewSsoUserResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                // Should automatically navigate to a logged in state.
            }
        }
    }

    private fun handleBackClick() {
        authRepository.logout()
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleRememberToggle(action: TrustedDeviceAction.RememberToggle) {
        mutableStateFlow.update { it.copy(isRemembered = action.isRemembered) }
    }

    private fun handleContinueClick() {
        mutableStateFlow.update {
            it.copy(
                dialogState = TrustedDeviceState.DialogState.Loading(R.string.loading.asText()),
            )
        }
        authRepository.shouldTrustDevice = state.isRemembered
        viewModelScope.launch {
            val result = authRepository.createNewSsoUser()
            sendAction(TrustedDeviceAction.Internal.ReceiveNewSsoUserResult(result))
        }
    }

    private fun handleApproveWithAdminClick() {
        authRepository.shouldTrustDevice = state.isRemembered
        sendEvent(TrustedDeviceEvent.NavigateToApproveWithAdmin(state.emailAddress))
    }

    private fun handleApproveWithDeviceClick() {
        authRepository.shouldTrustDevice = state.isRemembered
        sendEvent(TrustedDeviceEvent.NavigateToApproveWithDevice(state.emailAddress))
    }

    private fun handleApproveWithPasswordClick() {
        authRepository.shouldTrustDevice = state.isRemembered
        sendEvent(TrustedDeviceEvent.NavigateToLockScreen(state.emailAddress))
    }

    private fun handleNotYouClick() {
        authRepository.logout()
    }
}

/**
 * Models the state for the Trusted Device screen.
 */
@Parcelize
data class TrustedDeviceState(
    val dialogState: DialogState?,
    val emailAddress: String,
    val environmentLabel: String,
    val isRemembered: Boolean,
    val showContinueButton: Boolean,
    val showOtherDeviceButton: Boolean,
    val showRequestAdminButton: Boolean,
    val showMasterPasswordButton: Boolean,
) : Parcelable {
    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {
        /**
         * Represents a dismissible dialog with the given error [message].
         */
        @Parcelize
        data class Error(
            val title: Text?,
            val message: Text,
        ) : DialogState()

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models events for the Trusted Device screen.
 */
sealed class TrustedDeviceEvent {
    /**
     * Navigates to the approve with admin screen.
     */
    data class NavigateToApproveWithAdmin(
        val email: String,
    ) : TrustedDeviceEvent()

    /**
     * Navigates to the approve with device screen.
     */
    data class NavigateToApproveWithDevice(
        val email: String,
    ) : TrustedDeviceEvent()

    /**
     * Navigates to the lock screen.
     */
    data class NavigateToLockScreen(
        val email: String,
    ) : TrustedDeviceEvent()

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
     * User clicked to dismiss the dialog.
     */
    data object DismissDialog : TrustedDeviceAction()

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

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : TrustedDeviceAction() {
        /**
         * Indicates a new SSO user result has been received.
         */
        data class ReceiveNewSsoUserResult(
            val result: NewSsoUserResult,
        ) : Internal()
    }
}
