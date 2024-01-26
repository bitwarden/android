package com.x8bit.bitwarden.ui.auth.feature.loginwithdevice

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthRequestResult
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
 * Manages application state for the Login with Device screen.
 */
@HiltViewModel
class LoginWithDeviceViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LoginWithDeviceState, LoginWithDeviceEvent, LoginWithDeviceAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LoginWithDeviceState(
            emailAddress = LoginWithDeviceArgs(savedStateHandle).emailAddress,
            viewState = LoginWithDeviceState.ViewState.Loading,
        ),
) {
    init {
        sendNewAuthRequest()
    }

    override fun handleAction(action: LoginWithDeviceAction) {
        when (action) {
            LoginWithDeviceAction.CloseButtonClick -> handleCloseButtonClicked()
            LoginWithDeviceAction.ResendNotificationClick -> handleResendNotificationClicked()
            LoginWithDeviceAction.ViewAllLogInOptionsClick -> handleViewAllLogInOptionsClicked()

            is LoginWithDeviceAction.Internal.NewAuthRequestResultReceive -> {
                handleNewAuthRequestResultReceived(action)
            }
        }
    }

    private fun handleCloseButtonClicked() {
        sendEvent(LoginWithDeviceEvent.NavigateBack)
    }

    private fun handleResendNotificationClicked() {
        sendNewAuthRequest()
    }

    private fun handleViewAllLogInOptionsClicked() {
        sendEvent(LoginWithDeviceEvent.NavigateBack)
    }

    private fun handleNewAuthRequestResultReceived(
        action: LoginWithDeviceAction.Internal.NewAuthRequestResultReceive,
    ) {
        when (action.result) {
            is AuthRequestResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Content(
                            fingerprintPhrase = action.result.authRequest.fingerprint,
                            isResendNotificationLoading = false,
                        ),
                    )
                }
            }

            is AuthRequestResult.Error -> {
                // TODO BIT-1563 display error dialog
                mutableStateFlow.update {
                    it.copy(
                        viewState = LoginWithDeviceState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun sendNewAuthRequest() {
        setIsResendNotificationLoading(true)
        viewModelScope.launch {
            trySendAction(
                LoginWithDeviceAction.Internal.NewAuthRequestResultReceive(
                    result = authRepository.createAuthRequest(
                        email = state.emailAddress,
                    ),
                ),
            )
        }
    }

    private fun setIsResendNotificationLoading(isLoading: Boolean) {
        when (val viewState = mutableStateFlow.value.viewState) {
            is LoginWithDeviceState.ViewState.Content -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = viewState.copy(
                            isResendNotificationLoading = isLoading,
                        ),
                    )
                }
            }
            else -> Unit
        }
    }
}

/**
 * Models state of the Login with Device screen.
 */
@Parcelize
data class LoginWithDeviceState(
    val emailAddress: String,
    val viewState: ViewState,
) : Parcelable {
    /**
     * Represents the specific view states for the [LoginWithDeviceScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {
        /**
         * Loading state for the [LoginWithDeviceScreen], signifying that the content is being
         * processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a state where the [LoginWithDeviceScreen] is unable to display data due to an
         * error retrieving it.
         *
         * @property message The message to display on the error screen.
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Content state for the [LoginWithDeviceScreen] showing the actual content or items.
         *
         * @property fingerprintPhrase The fingerprint phrase to present to the user.
         */
        @Parcelize
        data class Content(
            val fingerprintPhrase: String,
            val isResendNotificationLoading: Boolean,
        ) : ViewState()
    }
}

/**
 * Models events for the Login with Device screen.
 */
sealed class LoginWithDeviceEvent {
    /**
     * Navigates back to the previous screen.
     */
    data object NavigateBack : LoginWithDeviceEvent()

    /**
     * Shows a toast with the given [message].
     */
    data class ShowToast(
        val message: String,
    ) : LoginWithDeviceEvent()
}

/**
 * Models actions for the Login with Device screen.
 */
sealed class LoginWithDeviceAction {
    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseButtonClick : LoginWithDeviceAction()

    /**
     * Indicates that the "Resend notification" text has been clicked.
     */
    data object ResendNotificationClick : LoginWithDeviceAction()

    /**
     * Indicates that the "View all log in options" text has been clicked.
     */
    data object ViewAllLogInOptionsClick : LoginWithDeviceAction()

    /**
     * Models actions for internal use by the view model.
     */
    sealed class Internal : LoginWithDeviceAction() {
        /**
         * A new auth request result was received.
         */
        data class NewAuthRequestResultReceive(
            val result: AuthRequestResult,
        ) : Internal()
    }
}
