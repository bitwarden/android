package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
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
 * View model for the Auto-fill setup screen.
 */
@HiltViewModel
class SetupAutoFillViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
) :
    BaseViewModel<SetupAutoFillState, SetupAutoFillEvent, SetupAutoFillAction>(
        // We load the state from the savedStateHandle for testing purposes.
        initialState = savedStateHandle[KEY_STATE] ?: run {
            val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
            val isInitialSetup = SetupAutoFillScreenArgs(savedStateHandle).isInitialSetup
            SetupAutoFillState(
                userId = userId,
                dialogState = null,
                autofillEnabled = false,
                isInitialSetup = isInitialSetup,
            )
        },
    ) {

    init {
        settingsRepository
            .isAutofillEnabledStateFlow
            .map {
                SetupAutoFillAction.Internal.AutofillEnabledUpdateReceive(isAutofillEnabled = it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SetupAutoFillAction) {
        when (action) {
            is SetupAutoFillAction.AutofillServiceChanged -> handleAutofillServiceChanged(action)
            SetupAutoFillAction.ContinueClick -> handleContinueClick()
            SetupAutoFillAction.DismissDialog -> handleDismissDialog()
            SetupAutoFillAction.TurnOnLaterClick -> handleTurnOnLaterClick()
            SetupAutoFillAction.AutoFillServiceFallback -> handleAutoFillServiceFallback()
            SetupAutoFillAction.TurnOnLaterConfirmClick -> handleTurnOnLaterConfirmClick()
            is SetupAutoFillAction.Internal.AutofillEnabledUpdateReceive -> {
                handleAutofillEnabledUpdateReceive(action)
            }

            SetupAutoFillAction.CloseClick -> handleCloseClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(SetupAutoFillEvent.NavigateBack)
    }

    private fun handleAutofillEnabledUpdateReceive(
        action: SetupAutoFillAction.Internal.AutofillEnabledUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(autofillEnabled = action.isAutofillEnabled)
        }
    }

    private fun handleAutoFillServiceFallback() {
        mutableStateFlow.update {
            it.copy(dialogState = SetupAutoFillDialogState.AutoFillFallbackDialog)
        }
    }

    private fun handleTurnOnLaterClick() {
        mutableStateFlow.update {
            it.copy(dialogState = SetupAutoFillDialogState.TurnOnLaterDialog)
        }
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update {
            it.copy(dialogState = null)
        }
    }

    private fun handleTurnOnLaterConfirmClick() {
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = true)
        updateOnboardingStatusToNextStep()
    }

    private fun handleContinueClick() {
        firstTimeActionManager.storeShowAutoFillSettingBadge(showBadge = false)
        if (state.isInitialSetup) {
            updateOnboardingStatusToNextStep()
        } else {
            sendEvent(SetupAutoFillEvent.NavigateBack)
        }
    }

    private fun handleAutofillServiceChanged(action: SetupAutoFillAction.AutofillServiceChanged) {
        if (action.autofillEnabled) {
            sendEvent(SetupAutoFillEvent.NavigateToAutofillSettings)
        } else {
            settingsRepository.disableAutofill()
        }
    }

    private fun updateOnboardingStatusToNextStep() =
        authRepository
            .setOnboardingStatus(
                userId = state.userId,
                status = OnboardingStatus.FINAL_STEP,
            )
}

/**
 * UI State for the Auto-fill setup screen.
 */
@Parcelize
data class SetupAutoFillState(
    val userId: String,
    val dialogState: SetupAutoFillDialogState?,
    val autofillEnabled: Boolean,
    val isInitialSetup: Boolean,
) : Parcelable

/**
 * Dialog states for the Auto-fill setup screen.
 */
sealed class SetupAutoFillDialogState : Parcelable {
    /**
     * Represents the turn on later dialog.
     */
    @Parcelize
    data object TurnOnLaterDialog : SetupAutoFillDialogState()

    /**
     * Represents the autofill fallback dialog.
     */
    @Parcelize
    data object AutoFillFallbackDialog : SetupAutoFillDialogState()
}

/**
 * UI Events for the Auto-fill setup screen.
 */
sealed class SetupAutoFillEvent {

    /**
     * Navigate to the autofill settings screen.
     */
    data object NavigateToAutofillSettings : SetupAutoFillEvent()

    /**
     * Navigate back.
     */
    data object NavigateBack : SetupAutoFillEvent()
}

/**
 * UI Actions for the Auto-fill setup screen.
 */
sealed class SetupAutoFillAction {
    /**
     * Dismiss the current dialog.
     */
    data object DismissDialog : SetupAutoFillAction()

    /**
     * Move on to the next set-up step.
     */
    data object ContinueClick : SetupAutoFillAction()

    /**
     * Turn autofill on later has been clicked.
     */
    data object TurnOnLaterClick : SetupAutoFillAction()

    /**
     * Turn autofill on later has been confirmed.
     */
    data object TurnOnLaterConfirmClick : SetupAutoFillAction()

    /**
     * Autofill service selection has changed.
     *
     * @param autofillEnabled Whether autofill is enabled.
     */
    data class AutofillServiceChanged(val autofillEnabled: Boolean) : SetupAutoFillAction()

    /**
     * Autofill service fallback has occurred.
     */
    data object AutoFillServiceFallback : SetupAutoFillAction()

    /**
     * The user has clicked the close button.
     */
    data object CloseClick : SetupAutoFillAction()

    /**
     * Internal actions not send through UI.
     */
    sealed class Internal : SetupAutoFillAction() {
        /**
         * An update for changes in the [isAutofillEnabled] value.
         */
        data class AutofillEnabledUpdateReceive(val isAutofillEnabled: Boolean) : Internal()
    }
}
