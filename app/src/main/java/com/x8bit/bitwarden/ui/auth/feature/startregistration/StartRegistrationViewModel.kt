package com.x8bit.bitwarden.ui.auth.feature.startregistration

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.RegisterResult
import com.x8bit.bitwarden.data.auth.repository.model.SendVerificationEmailResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.model.Environment.Type
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.BackClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ContinueClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EmailInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.EnvironmentTypeSelect
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ErrorDialogDismiss
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.Internal.OnboardingFeatureFlagUpdated
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.Internal.ReceiveSendVerificationEmailResult
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.Internal.UpdatedEnvironmentReceive
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.NameInputChange
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.PrivacyPolicyClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ReceiveMarketingEmailsToggle
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.ServerGeologyHelpClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.TermsClick
import com.x8bit.bitwarden.ui.auth.feature.startregistration.StartRegistrationAction.UnsubscribeMarketingEmailsClick
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.isValidEmail
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
    featureFlagManager: FeatureFlagManager,
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
            showNewOnboardingUi = featureFlagManager.getFeatureFlag(FlagKey.OnboardingFlow),
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
            .onEach { environment ->
                sendAction(
                    UpdatedEnvironmentReceive(environment = environment),
                )
            }
            .launchIn(viewModelScope)
        // Listen for changes on the onboarding feature flag.
        featureFlagManager
            .getFeatureFlagFlow(FlagKey.OnboardingFlow)
            .map {
                OnboardingFeatureFlagUpdated(it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: StartRegistrationAction) {
        when (action) {
            is ContinueClick -> handleContinueClick()
            is EmailInputChange -> handleEmailInputChanged(action)
            is NameInputChange -> handleNameInputChanged(action)
            is BackClick -> handleBackClick()
            is ErrorDialogDismiss -> handleDialogDismiss()
            is ReceiveMarketingEmailsToggle -> handleReceiveMarketingEmailsToggle(
                action,
            )

            is PrivacyPolicyClick -> handlePrivacyPolicyClick()
            is TermsClick -> handleTermsClick()
            is UnsubscribeMarketingEmailsClick -> handleUnsubscribeMarketingEmailsClick()
            is ReceiveSendVerificationEmailResult -> {
                handleReceiveSendVerificationEmailResult(action)
            }

            is EnvironmentTypeSelect -> handleEnvironmentTypeSelect(action)
            is UpdatedEnvironmentReceive -> {
                handleUpdatedEnvironmentReceive(action)
            }

            ServerGeologyHelpClick -> handleServerGeologyHelpClick()
            is OnboardingFeatureFlagUpdated -> handleOnboardingFeatureFlagUpdated(action)
        }
    }

    private fun handleOnboardingFeatureFlagUpdated(action: OnboardingFeatureFlagUpdated) {
        mutableStateFlow.update {
            it.copy(showNewOnboardingUi = action.newValue)
        }
    }

    private fun handleServerGeologyHelpClick() {
        sendEvent(StartRegistrationEvent.NavigateToServerSelectionInfo)
    }

    private fun handleEnvironmentTypeSelect(action: EnvironmentTypeSelect) {
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

    private fun handleUpdatedEnvironmentReceive(
        action: UpdatedEnvironmentReceive,
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

    private fun handleReceiveMarketingEmailsToggle(action: ReceiveMarketingEmailsToggle) {
        mutableStateFlow.update {
            it.copy(isReceiveMarketingEmailsToggled = action.newState)
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    private fun handleBackClick() {
        sendEvent(StartRegistrationEvent.NavigateBack)
    }

    private fun handleEmailInputChanged(action: EmailInputChange) {
        mutableStateFlow.update {
            it.copy(
                emailInput = action.input,
                isContinueButtonEnabled = action.input.isNotBlank(),
            )
        }
    }

    private fun handleNameInputChanged(action: NameInputChange) {
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
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.validation_field_required
                            .asText(R.string.email_address.asText()),
                    ),
                )
            }
        }

        !state.emailInput.isValidEmail() -> {
            mutableStateFlow.update {
                it.copy(
                    dialog = StartRegistrationDialog.Error(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.invalid_email.asText(),
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
                ReceiveSendVerificationEmailResult(
                    sendVerificationEmailResult = result,
                ),
            )
        }
    }

    private fun handleReceiveSendVerificationEmailResult(
        result: ReceiveSendVerificationEmailResult,
    ) {
        when (val sendVerificationEmailResult = result.sendVerificationEmailResult) {

            is SendVerificationEmailResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = StartRegistrationDialog.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = sendVerificationEmailResult
                                .errorMessage
                                ?.asText()
                                ?: R.string.generic_error_message.asText(),
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
    val showNewOnboardingUi: Boolean,
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
     * Placeholder event for showing a toast. Can be removed once there are real events.
     */
    data class ShowToast(val text: String) : StartRegistrationEvent()

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
     * User clicked back.
     */
    data object BackClick : StartRegistrationAction()

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
         * Indicates that there has been a change in [environment].
         */
        data class UpdatedEnvironmentReceive(
            val environment: Environment,
        ) : Internal()

        /**
         * Indicates updated value for onboarding feature flag.
         */
        data class OnboardingFeatureFlagUpdated(val newValue: Boolean) : Internal()
    }
}
