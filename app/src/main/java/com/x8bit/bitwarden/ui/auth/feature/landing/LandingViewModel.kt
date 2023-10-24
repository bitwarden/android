package com.x8bit.bitwarden.ui.auth.feature.landing

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.isValidEmail
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Manages application state for the initial landing screen.
 */
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LandingState, LandingEvent, LandingAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LandingState(
            emailInput = authRepository.rememberedEmailAddress.orEmpty(),
            isContinueButtonEnabled = authRepository.rememberedEmailAddress != null,
            isRememberMeEnabled = authRepository.rememberedEmailAddress != null,
            selectedEnvironment = environmentRepository.environment,
            errorDialogState = BasicDialogState.Hidden,
        ),
) {

    init {
        // As state updates:
        // - write to saved state handle
        // - updated selected environment
        stateFlow
            .onEach {
                savedStateHandle[KEY_STATE] = it
                environmentRepository.environment = it.selectedEnvironment
            }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LandingAction) {
        when (action) {
            is LandingAction.ContinueButtonClick -> handleContinueButtonClicked()
            LandingAction.CreateAccountClick -> handleCreateAccountClicked()
            is LandingAction.ErrorDialogDismiss -> handleErrorDialogDismiss()
            is LandingAction.RememberMeToggle -> handleRememberMeToggled(action)
            is LandingAction.EmailInputChanged -> handleEmailInputUpdated(action)
            is LandingAction.EnvironmentTypeSelect -> handleEnvironmentTypeSelect(action)
        }
    }

    private fun handleEmailInputUpdated(action: LandingAction.EmailInputChanged) {
        val email = action.input
        mutableStateFlow.update {
            it.copy(
                emailInput = email,
                isContinueButtonEnabled = email.isNotBlank(),
            )
        }
    }

    private fun handleContinueButtonClicked() {
        if (!mutableStateFlow.value.emailInput.isValidEmail()) {
            mutableStateFlow.update {
                it.copy(
                    errorDialogState = BasicDialogState.Shown(
                        title = R.string.an_error_has_occurred.asText(),
                        message = R.string.invalid_email.asText(),
                    ),
                )
            }
            return
        }

        val email = mutableStateFlow.value.emailInput
        val isRememberMeEnabled = mutableStateFlow.value.isRememberMeEnabled

        // Update the remembered email address
        authRepository.rememberedEmailAddress = email.takeUnless { !isRememberMeEnabled }

        sendEvent(LandingEvent.NavigateToLogin(email))
    }

    private fun handleCreateAccountClicked() {
        sendEvent(LandingEvent.NavigateToCreateAccount)
    }

    private fun handleErrorDialogDismiss() {
        mutableStateFlow.update {
            it.copy(errorDialogState = BasicDialogState.Hidden)
        }
    }

    private fun handleRememberMeToggled(action: LandingAction.RememberMeToggle) {
        mutableStateFlow.update { it.copy(isRememberMeEnabled = action.isChecked) }
    }

    private fun handleEnvironmentTypeSelect(action: LandingAction.EnvironmentTypeSelect) {
        val environment = when (action.environmentType) {
            Environment.Type.US -> Environment.Us
            Environment.Type.EU -> Environment.Eu
            Environment.Type.SELF_HOSTED -> {
                // TODO Show dialog for setting selected environment (BIT-330)
                Environment.SelfHosted(
                    environmentUrlData = Environment.Us.environmentUrlData,
                )
            }
        }

        mutableStateFlow.update {
            it.copy(
                selectedEnvironment = environment,
            )
        }
    }
}

/**
 * Models state of the landing screen.
 */
@Parcelize
data class LandingState(
    val emailInput: String,
    val isContinueButtonEnabled: Boolean,
    val isRememberMeEnabled: Boolean,
    val selectedEnvironment: Environment,
    val errorDialogState: BasicDialogState,
) : Parcelable

/**
 * Models events for the landing screen.
 */
sealed class LandingEvent {
    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateToCreateAccount : LandingEvent()

    /**
     * Navigates to the Login screen with the given email address and region label.
     */
    data class NavigateToLogin(
        val emailAddress: String,
    ) : LandingEvent()
}

/**
 * Models actions for the landing screen.
 */
sealed class LandingAction {
    /**
     * Indicates that the continue button has been clicked and the app should navigate to Login.
     */
    data object ContinueButtonClick : LandingAction()

    /**
     * Indicates that the Create Account text was clicked.
     */
    data object CreateAccountClick : LandingAction()

    /**
     * Indicates that an error dialog is attempting to be dismissed.
     */
    data object ErrorDialogDismiss : LandingAction()

    /**
     * Indicates that the Remember Me switch has been toggled.
     */
    data class RememberMeToggle(
        val isChecked: Boolean,
    ) : LandingAction()

    /**
     * Indicates that the input on the email field has changed.
     */
    data class EmailInputChanged(
        val input: String,
    ) : LandingAction()

    /**
     * Indicates that the selection from the region drop down has changed.
     */
    data class EnvironmentTypeSelect(
        val environmentType: Environment.Type,
    ) : LandingAction()
}
