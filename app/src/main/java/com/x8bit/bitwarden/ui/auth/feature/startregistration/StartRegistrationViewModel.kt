package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.NameInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.CloseClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.TermsClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.isValidEmail
import com.x8bit.bitwarden.ui.platform.components.dialog.BasicDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Models logic for the create account screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class StartRegistrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
) : BaseViewModel<StartRegistrationState, StartRegistrationEvent, StartRegistrationAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: StartRegistrationState(
            emailInput = "",
            nameInput = "",
            isReceiveMarketingEmailsToggled = environmentRepository.environment.type == Environment.Type.US,
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
        authRepository
            .captchaTokenResultFlow
            .onEach {
                sendAction(
                    StartRegistrationAction.Internal.ReceiveCaptchaToken(
                        tokenResult = it,
                    ),
                )
            }
            .launchIn(viewModelScope)

        // Listen for changes in environment triggered both by this VM and externally.
        environmentRepository
            .environmentStateFlow
            .onEach { environment ->
                sendAction(
                    StartRegistrationAction.Internal.UpdatedEnvironmentReceive(environment = environment),
                )
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StartRegistrationAction) {
        when (action) {
            is StartRegistrationAction.ContinueClick -> handleContinueClick()
            is EmailInputChange -> handleEmailInputChanged(action)
            is NameInputChange -> handleNameInputChanged(action)
            is CloseClick -> handleCloseClick()
            is ErrorDialogDismiss -> handleDialogDismiss()
            is StartRegistrationAction.ReceiveMarketingEmailsToggle -> handleReceiveMarketingEmailsToggle(action)
            is PrivacyPolicyClick -> handlePrivacyPolicyClick()
            is TermsClick -> handleTermsClick()
            is StartRegistrationAction.UnsubscribeMarketingEmailsClick -> handleUnsubscribeMarketingEmailsClick()
            is StartRegistrationAction.Internal.ReceiveRegisterResult -> {
                // handleReceiveRegisterAccountResult(action)
            }
            is StartRegistrationAction.Internal.ReceiveCaptchaToken -> {
                handleReceiveCaptchaToken(action)
            }

            is StartRegistrationAction.EnvironmentTypeSelect -> handleEnvironmentTypeSelect(action)
            is StartRegistrationAction.Internal.UpdatedEnvironmentReceive -> {
            handleUpdatedEnvironmentReceive(action)
        }
        }
    }

    private fun handleReceiveCaptchaToken(
        action: StartRegistrationAction.Internal.ReceiveCaptchaToken,
    ) {
        when (val result = action.tokenResult) {
            is CaptchaCallbackTokenResult.MissingToken -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = StartRegistrationDialog.Error(
                            BasicDialogState.Shown(
                                title = R.string.an_error_has_occurred.asText(),
                                message = R.string.captcha_failed.asText(),
                            ),
                        ),
                    )
                }
            }

            is CaptchaCallbackTokenResult.Success -> {
                submitRegisterAccountRequest(
                    shouldCheckForDataBreaches = false,
                    shouldIgnorePasswordStrength = true,
                    captchaToken = result.token,
                )
            }
        }
    }

    private fun handleEnvironmentTypeSelect(action: StartRegistrationAction.EnvironmentTypeSelect) {
        val environment = when (action.environmentType) {
            Environment.Type.US -> Environment.Us
            Environment.Type.EU -> Environment.Eu
            Environment.Type.SELF_HOSTED -> {
                // Launch the self-hosted screen and select the full environment details there.
                sendEvent(StartRegistrationEvent.NavigateToEnvironment)
                return
            }
        }

        // Update the environment in the repo; the VM state will update accordingly because it is
        // listening for changes.
        environmentRepository.environment = environment
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

    private fun handlePrivacyPolicyClick() = sendEvent(StartRegistrationEvent.NavigateToPrivacyPolicy)

    private fun handleTermsClick() = sendEvent(StartRegistrationEvent.NavigateToTerms)

    private fun handleUnsubscribeMarketingEmailsClick() = sendEvent(StartRegistrationEvent.NavigateToUnsubscribe)

    private fun handleReceiveMarketingEmailsToggle(action: StartRegistrationAction.ReceiveMarketingEmailsToggle) {
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

    private fun handleEmailInputChanged(action: EmailInputChange) {
        mutableStateFlow.update {
            it.copy(
                emailInput = action.input,
                isContinueButtonEnabled = action.input.isNotBlank() && state.nameInput.isNotBlank()
            )
        }
    }

    private fun handleNameInputChanged(action: NameInputChange) {
        mutableStateFlow.update {
            it.copy(
                nameInput = action.input,
                isContinueButtonEnabled = action.input.isNotBlank() && state.emailInput.isNotBlank()
            )
        }
    }

    private fun handleContinueClick() = when {
        state.emailInput.isBlank() -> {
            val dialog = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required
                    .asText(R.string.email_address.asText()),
            )
            mutableStateFlow.update { it.copy(dialog = StartRegistrationDialog.Error(dialog)) }
        }

        !state.emailInput.isValidEmail() -> {
            val dialog = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.invalid_email.asText(),
            )
            mutableStateFlow.update { it.copy(dialog = StartRegistrationDialog.Error(dialog)) }
        }

        state.nameInput.isBlank() -> {
            val dialog = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = R.string.validation_field_required
                    .asText(R.string.name.asText()),
            )
            mutableStateFlow.update { it.copy(dialog = StartRegistrationDialog.Error(dialog)) }
        }

        else -> {
            // TODO Call to send verification email
           /*
           submitRegisterAccountRequest(
                shouldCheckForDataBreaches = state.isCheckDataBreachesToggled,
                shouldIgnorePasswordStrength = false,
                captchaToken = null,
            )
            */

            viewModelScope.launch {
                sendEvent(StartRegistrationEvent.NavigateToCompleteRegistration(
                    email = state.emailInput,
                    verificationToken = "",
                    captchaToken = ""
                ))
            }
        }
    }

    private fun submitRegisterAccountRequest(
        shouldCheckForDataBreaches: Boolean,
        shouldIgnorePasswordStrength: Boolean,
        captchaToken: String?,
    ) {
        mutableStateFlow.update {
            it.copy(dialog = StartRegistrationDialog.Loading)
        }
        viewModelScope.launch {
            // TODO change to send email service call
            /*
            val result = authRepository.register(
                email = state.emailInput,
                captchaToken = captchaToken,
            )
            sendAction(
                StartRegistrationAction.Internal.ReceiveRegisterResult(
                    registerResult = result,
                ),
            )*/
        }
    }
}

/**
 * UI state for the create account screen.
 */
@Parcelize
data class StartRegistrationState(
    val emailInput: String,
    val nameInput: String,
    val isReceiveMarketingEmailsToggled: Boolean,
    val isContinueButtonEnabled: Boolean,
    val selectedEnvironmentType: Environment.Type,
    val dialog: StartRegistrationDialog?
) : Parcelable {

}

/**
 * Models dialogs that can be displayed on the create account screen.
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
    data class Error(val state: BasicDialogState.Shown) : StartRegistrationDialog()
}

/**
 * Models events for the create account screen.
 */
sealed class StartRegistrationEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : StartRegistrationEvent()

    /**
     * Placeholder event for showing a toast. Can be removed once there are real events.
     */
    data class ShowToast(val text: String) : StartRegistrationEvent()

    /**
     * Navigates to the captcha verification screen.
     */
    data class NavigateToCaptcha(val uri: Uri) : StartRegistrationEvent()

    /**
     * Navigates to the complete registration screen.
     */
    data class NavigateToCompleteRegistration(
        val email: String,
        val verificationToken: String,
        val captchaToken: String,
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
    data object NavigateToUnsubscribe: StartRegistrationEvent()

    /**
     * Navigates to the self-hosted/custom environment screen.
     */
    data object NavigateToEnvironment : StartRegistrationEvent()
}

/**
 * Models actions for the create account screen.
 */
sealed class StartRegistrationAction {
    /**
     * User clicked submit.
     */
    data object ContinueClick : StartRegistrationAction()

    /**
     * User clicked close.
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
        val environmentType: Environment.Type,
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
     * Models actions that the [StartRegistrationViewModel] itself might send.
     */
    sealed class Internal : StartRegistrationAction() {
        /**
         * Indicates a captcha callback token has been received.
         */
        data class ReceiveCaptchaToken(
            val tokenResult: CaptchaCallbackTokenResult,
        ) : Internal()

        /**
         * Indicates a [RegisterResult] has been received.
         */
        data class ReceiveRegisterResult(
            val registerResult: RegisterResult,
        ) : Internal()

        /**
         * Indicates that there has been a change in [environment].
         */
        data class UpdatedEnvironmentReceive(
            val environment: Environment,
        ) : Internal()
    }
}
