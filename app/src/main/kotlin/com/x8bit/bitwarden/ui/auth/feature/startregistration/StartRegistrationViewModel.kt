package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.model.Environment.Type
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.util.isValidEmail
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.ui.platform.model.SnackbarRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Models logic for the start registration screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class StartRegistrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
) : BaseViewModel<StartRegistrationState, StartRegistrationEvent, StartRegistrationAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: StartRegistrationState(
            emailInput = "",
            nameInput = "",
            isReceiveMarketingEmailsToggled = environmentRepository.environment.type == Type.US,
            isContinueButtonEnabled = false,
            selectedEnvironmentType = environmentRepository.environment.type,
            dialog = null,
        ),
) {

    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        // Listen for changes in environment triggered both by this VM and externally.
        environmentRepository
            .environmentStateFlow
            .map { StartRegistrationAction.Internal.UpdatedEnvironmentReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.ENVIRONMENT_SAVED)
            .map { StartRegistrationAction.Internal.SnackbarDataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StartRegistrationAction) {
        when (action) {
            is StartRegistrationAction.ContinueClick -> handleContinueClick()
            is StartRegistrationAction.EmailInputChange -> handleEmailInputChanged(action)
            is StartRegistrationAction.NameInputChange -> handleNameInputChanged(action)
            is StartRegistrationAction.CloseClick -> handleCloseClick()
            is StartRegistrationAction.ErrorDialogDismiss -> handleDialogDismiss()
            is StartRegistrationAction.ReceiveMarketingEmailsToggle -> {
                handleReceiveMarketingEmailsToggle(action)
            }

            is StartRegistrationAction.PrivacyPolicyClick -> handlePrivacyPolicyClick()
            is StartRegistrationAction.TermsClick -> handleTermsClick()
            is StartRegistrationAction.UnsubscribeMarketingEmailsClick -> {
                handleUnsubscribeMarketingEmailsClick()
            }

            is StartRegistrationAction.EnvironmentTypeSelect -> handleEnvironmentTypeSelect(action)
            StartRegistrationAction.ServerGeologyHelpClick -> handleServerGeologyHelpClick()
            is StartRegistrationAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: StartRegistrationAction.Internal) {
        when (action) {
            is StartRegistrationAction.Internal.ReceiveSendVerificationEmailResult -> {
                handleReceiveSendVerificationEmailResult(action)
            }

            is StartRegistrationAction.Internal.SnackbarDataReceived -> {
                handleSnackbarDataReceived(action)
            }

            is StartRegistrationAction.Internal.UpdatedEnvironmentReceive -> {
                handleUpdatedEnvironmentReceive(action)
            }
        }
    }

    private fun handleServerGeologyHelpClick() {
        sendEvent(StartRegistrationEvent.NavigateToServerSelectionInfo)
    }

    private fun handleEnvironmentTypeSelect(action: StartRegistrationAction.EnvironmentTypeSelect) {
        val environment = when (action.environmentType) {
            Type.US -> Environment.Us
            Type.EU -> Environment.Eu
            Type.SELF_HOSTED -> {
                // Launch the self-hosted screen and select the full environment details there.
                sendEvent(StartRegistrationEvent.NavigateToEnvironment)
                return
            }
        }

        // Update the environment in the repo; the VM state will update accordingly because it is
        // listening for changes.
        environmentRepository.environment = environment
    }

    private fun handleSnackbarDataReceived(
        action: StartRegistrationAction.Internal.SnackbarDataReceived,
    ) {
        sendEvent(StartRegistrationEvent.ShowSnackbar(action.data))
    }

    private fun handleUpdatedEnvironmentReceive(
        action: StartRegistrationAction.Internal.UpdatedEnvironmentReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                selectedEnvironmentType = action.environment.type,
            )
        }
    }

    private fun handlePrivacyPolicyClick() =
        sendEvent(StartRegistrationEvent.NavigateToPrivacyPolicy)

    private fun handleTermsClick() = sendEvent(StartRegistrationEvent.NavigateToTerms)

    private fun handleUnsubscribeMarketingEmailsClick() =
        sendEvent(StartRegistrationEvent.NavigateToUnsubscribe)

    private fun handleReceiveMarketingEmailsToggle(
        action: StartRegistrationAction.ReceiveMarketingEmailsToggle,
    ) {
        mutableStateFlow.update {
            it.copy(isReceiveMarketingEmailsToggled = action.newState)
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleCloseClick() {
        sendEvent(StartRegistrationEvent.NavigateBack)
    }

    private fun handleEmailInputChanged(action: StartRegistrationAction.EmailInputChange) {
        mutableStateFlow.update {
            it.copy(
                emailInput = action.input,
                isContinueButtonEnabled = action.input.isNotBlank(),
            )
        }
    }

    private fun handleNameInputChanged(action: StartRegistrationAction.NameInputChange) {
        mutableStateFlow.update {
            it.copy(
                nameInput = action.input,
                isContinueButtonEnabled = state.emailInput.isNotBlank(),
            )
        }
    }

    private fun handleContinueClick() = when {
        state.emailInput.isBlank() -> {
            mutableStateFlow.update {
                it.copy(
                    dialog = StartRegistrationDialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.validation_field_required
                            .asText(BitwardenString.email_address.asText()),
                    ),
                )
            }
        }

        !state.emailInput.isValidEmail() -> {
            mutableStateFlow.update {
                it.copy(
                    dialog = StartRegistrationDialog.Error(
                        title = BitwardenString.an_error_has_occurred.asText(),
                        message = BitwardenString.invalid_email.asText(),
                    ),
                )
            }
        }

        else -> {
            submitSendVerificationEmailRequest()
        }
    }

    private fun submitSendVerificationEmailRequest() {
        mutableStateFlow.update {
            it.copy(dialog = StartRegistrationDialog.Loading)
        }
        viewModelScope.launch {
            val result = authRepository.sendVerificationEmail(
                email = state.emailInput,
                name = state.nameInput,
                receiveMarketingEmails = state.isReceiveMarketingEmailsToggled,
            )
            sendAction(
                StartRegistrationAction.Internal.ReceiveSendVerificationEmailResult(
                    sendVerificationEmailResult = result,
                ),
            )
        }
    }

    private fun handleReceiveSendVerificationEmailResult(
        result: StartRegistrationAction.Internal.ReceiveSendVerificationEmailResult,
    ) {
        when (val sendVerificationEmailResult = result.sendVerificationEmailResult) {
            is SendVerificationEmailResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = StartRegistrationDialog.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = sendVerificationEmailResult
                                .errorMessage
                                ?.asText()
                                ?: BitwardenString.generic_error_message.asText(),
                            error = sendVerificationEmailResult.error,
                        ),
                    )
                }
            }

            is SendVerificationEmailResult.Success -> {
                environmentRepository.saveCurrentEnvironmentForEmail(state.emailInput)
                mutableStateFlow.update { it.copy(dialog = null) }
                if (sendVerificationEmailResult.emailVerificationToken == null) {
                    sendEvent(
                        StartRegistrationEvent.NavigateToCheckEmail(
                            email = state.emailInput,
                        ),
                    )
                } else {
                    sendEvent(
                        StartRegistrationEvent.NavigateToCompleteRegistration(
                            email = state.emailInput,
                            verificationToken = sendVerificationEmailResult.emailVerificationToken,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * UI state for the start registration screen.
 */
@Parcelize
data class StartRegistrationState(
    val emailInput: String,
    val nameInput: String,
    val isReceiveMarketingEmailsToggled: Boolean,
    val isContinueButtonEnabled: Boolean,
    val selectedEnvironmentType: Type,
    val dialog: StartRegistrationDialog?,
) : Parcelable

/**
 * Models dialogs that can be displayed on the start registration screen.
 */
sealed class StartRegistrationDialog : Parcelable {
    /**
     * Loading dialog.
     */
    @Parcelize
    data object Loading : StartRegistrationDialog()

    /**
     * General error dialog with an OK button.
     */
    @Parcelize
    data class Error(
        val title: Text?,
        val message: Text,
        val error: Throwable? = null,
    ) : StartRegistrationDialog()
}

/**
 * Models events for the start registration screen.
 */
sealed class StartRegistrationEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : StartRegistrationEvent()

    /**
     * Navigates to the complete registration screen.
     */
    data class NavigateToCompleteRegistration(
        val email: String,
        val verificationToken: String,
    ) : StartRegistrationEvent()

    /**
     * Navigates to the check email screen.
     */
    data class NavigateToCheckEmail(
        val email: String,
    ) : StartRegistrationEvent()

    /**
     * Navigate to terms and conditions.
     */
    data object NavigateToTerms : StartRegistrationEvent()

    /**
     * Navigate to privacy policy.
     */
    data object NavigateToPrivacyPolicy : StartRegistrationEvent()

    /**
     * Navigate to unsubscribe to marketing emails.
     */
    data object NavigateToUnsubscribe : StartRegistrationEvent()

    /**
     * Navigates to the self-hosted/custom environment screen.
     */
    data object NavigateToEnvironment : StartRegistrationEvent()

    /**
     * Navigates to the server selection info.
     */
    data object NavigateToServerSelectionInfo : StartRegistrationEvent()

    /**
     * Show a snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : StartRegistrationEvent(), BackgroundEvent
}

/**
 * Models actions for the start registration screen.
 */
sealed class StartRegistrationAction {
    /**
     * User clicked continue.
     */
    data object ContinueClick : StartRegistrationAction()

    /**
     * Indicates that the top-bar close button was clicked.
     */
    data object CloseClick : StartRegistrationAction()

    /**
     * Email input changed.
     */
    data class EmailInputChange(val input: String) : StartRegistrationAction()

    /**
     * Name input changed.
     */
    data class NameInputChange(val input: String) : StartRegistrationAction()

    /**
     * Indicates that the selection from the region drop down has changed.
     */
    data class EnvironmentTypeSelect(
        val environmentType: Type,
    ) : StartRegistrationAction()

    /**
     * User dismissed the error dialog.
     */
    data object ErrorDialogDismiss : StartRegistrationAction()

    /**
     * User tapped receive marketing emails toggle.
     */
    data class ReceiveMarketingEmailsToggle(val newState: Boolean) : StartRegistrationAction()

    /**
     * User tapped privacy policy link.
     */
    data object PrivacyPolicyClick : StartRegistrationAction()

    /**
     * User tapped terms link.
     */
    data object TermsClick : StartRegistrationAction()

    /**
     * User tapped the unsubscribe link.
     */
    data object UnsubscribeMarketingEmailsClick : StartRegistrationAction()

    /**
     * User has tapped the tooltip for the server environment
     */
    data object ServerGeologyHelpClick : StartRegistrationAction()

    /**
     * Models actions that the [StartRegistrationViewModel] itself might send.
     */
    sealed class Internal : StartRegistrationAction() {
        /**
         * Indicates a [RegisterResult] has been received.
         */
        data class ReceiveSendVerificationEmailResult(
            val sendVerificationEmailResult: SendVerificationEmailResult,
        ) : Internal()

        /**
         * Indicates that snackbar data has been received.
         */
        data class SnackbarDataReceived(
            val data: BitwardenSnackbarData,
        ) : Internal()

        /**
         * Indicates that there has been a change in [environment].
         */
        data class UpdatedEnvironmentReceive(
            val environment: Environment,
        ) : Internal()
    }
}
