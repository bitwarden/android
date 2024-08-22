package com.x8bit.bitwarden.ui.auth.feature.checkemail

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * Models logic for the check email screen.
 */
@HiltViewModel
class CheckEmailViewModel @Inject constructor(
    featureFlagManager: FeatureFlagManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<CheckEmailState, CheckEmailEvent, CheckEmailAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: CheckEmailState(
            email = CheckEmailArgs(savedStateHandle).emailAddress,
            showNewOnboardingUi = featureFlagManager.getFeatureFlag(FlagKey.OnboardingFlow),
        ),
) {
    init {
        // As state updates, write to saved state handle:
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        // Listen for changes on the onboarding feature flag.
        featureFlagManager
            .getFeatureFlagFlow(FlagKey.OnboardingFlow)
            .map {
                CheckEmailAction.Internal.OnboardingFeatureFlagUpdated(it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: CheckEmailAction) {
        when (action) {
            CheckEmailAction.BackClick -> handleBackClick()
            CheckEmailAction.OpenEmailClick -> handleOpenEmailClick()
            CheckEmailAction.ChangeEmailClick -> handleChangeEmailClick()
            is CheckEmailAction.Internal.OnboardingFeatureFlagUpdated -> {
                handleOnboardingFeatureFlagUpdated(action)
            }

            CheckEmailAction.LoginClick -> handleLoginClick()
        }
    }

    private fun handleLoginClick() {
        sendEvent(CheckEmailEvent.NavigateBackToLanding)
    }

    private fun handleOnboardingFeatureFlagUpdated(
        action: CheckEmailAction.Internal.OnboardingFeatureFlagUpdated,
    ) {
        mutableStateFlow.update {
            it.copy(showNewOnboardingUi = action.newValue)
        }
    }

    private fun handleOpenEmailClick() = sendEvent(CheckEmailEvent.NavigateToEmailApp)

    private fun handleBackClick() = sendEvent(CheckEmailEvent.NavigateBack)

    private fun handleChangeEmailClick() = sendEvent(CheckEmailEvent.NavigateBack)
}

/**
 * UI state for the check email screen.
 */
@Parcelize
data class CheckEmailState(
    val email: String,
    val showNewOnboardingUi: Boolean,
) : Parcelable

/**
 * Models events for the check email screen.
 */
sealed class CheckEmailEvent {

    /**
     * Navigate back to previous screen.
     */
    data object NavigateBack : CheckEmailEvent()

    /**
     * Navigate to email app.
     */
    data object NavigateToEmailApp : CheckEmailEvent()

    /**
     * Navigate back to Landing
     */
    data object NavigateBackToLanding : CheckEmailEvent()
}

/**
 * Models actions for the check email screen.
 */
sealed class CheckEmailAction {
    /**
     * User clicked close.
     */
    data object BackClick : CheckEmailAction()

    /**
     * User clicked change email.
     */
    data object ChangeEmailClick : CheckEmailAction()

    /**
     * User clicked open email.
     */
    data object OpenEmailClick : CheckEmailAction()

    /**
     * User clicked log in.
     */
    data object LoginClick : CheckEmailAction()

    /**
     * Denotes an internal action.
     */
    sealed class Internal : CheckEmailAction() {
        /**
         * Indicates updated value for onboarding feature flag.
         */
        data class OnboardingFeatureFlagUpdated(val newValue: Boolean) : Internal()
    }
}
