package com.x8bit.bitwarden.ui.auth.feature.landing

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
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
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<LandingState, LandingEvent, LandingAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: LandingState(
            emailInput = authRepository.rememberedEmailAddress.orEmpty(),
            isContinueButtonEnabled = authRepository.rememberedEmailAddress != null,
            isRememberMeEnabled = authRepository.rememberedEmailAddress != null,
            selectedRegion = LandingState.RegionOption.BITWARDEN_US,
        ),
) {

    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: LandingAction) {
        when (action) {
            is LandingAction.ContinueButtonClick -> handleContinueButtonClicked()
            LandingAction.CreateAccountClick -> handleCreateAccountClicked()
            is LandingAction.RememberMeToggle -> handleRememberMeToggled(action)
            is LandingAction.EmailInputChanged -> handleEmailInputUpdated(action)
            is LandingAction.RegionOptionSelect -> handleRegionSelect(action)
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
        // TODO: add actual validation here: BIT-193
        if (mutableStateFlow.value.emailInput.isBlank()) {
            return
        }

        val email = mutableStateFlow.value.emailInput
        val isRememberMeEnabled = mutableStateFlow.value.isRememberMeEnabled

        // Update the remembered email address
        authRepository.rememberedEmailAddress = email.takeUnless { !isRememberMeEnabled }
        // Update the selected region selectedRegionLabel
        authRepository.selectedRegionLabel = mutableStateFlow.value.selectedRegion.label

        sendEvent(LandingEvent.NavigateToLogin(email))
    }

    private fun handleCreateAccountClicked() {
        sendEvent(LandingEvent.NavigateToCreateAccount)
    }

    private fun handleRememberMeToggled(action: LandingAction.RememberMeToggle) {
        mutableStateFlow.update { it.copy(isRememberMeEnabled = action.isChecked) }
    }

    private fun handleRegionSelect(action: LandingAction.RegionOptionSelect) {
        mutableStateFlow.update {
            it.copy(
                selectedRegion = action.regionOption,
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
    val selectedRegion: RegionOption,
) : Parcelable {
    /**
     * Enumerates the possible region options with their corresponding labels.
     */
    enum class RegionOption(val label: String) {
        BITWARDEN_US("bitwarden.com"),
        BITWARDEN_EU("bitwarden.eu"),
        SELF_HOSTED("Self-hosted"),
    }
}

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
    data class RegionOptionSelect(
        val regionOption: LandingState.RegionOption,
    ) : LandingAction()
}
