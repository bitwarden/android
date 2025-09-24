package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.model.BrowserAutofillSettingsOption
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.util.toBrowserAutoFillSettingsOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the Setup Browser Autofill screen.
 */
@HiltViewModel
class SetupBrowserAutofillViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SetupBrowserAutofillState, SetupBrowserAutofillEvent, SetupBrowserAutofillAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: SetupBrowserAutofillState(
        dialogState = null,
        browserAutofillSettingsOptions = browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatus
            .toBrowserAutoFillSettingsOptions(),
    ),
) {
    init {
        browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatusFlow
            .map(SetupBrowserAutofillAction.Internal::BrowserAutofillStatusReceive)
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SetupBrowserAutofillAction) {
        when (action) {
            is SetupBrowserAutofillAction.BrowserIntegrationClick -> {
                handleBrowserIntegrationClick(action)
            }

            SetupBrowserAutofillAction.DismissDialog -> handleDismissDialog()
            SetupBrowserAutofillAction.ContinueClick -> handleContinueClick()
            SetupBrowserAutofillAction.TurnOnLaterClick -> handleTurnOnLaterClick()
            SetupBrowserAutofillAction.TurnOnLaterConfirmClick -> handleTurnOnLaterConfirmClick()
            is SetupBrowserAutofillAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: SetupBrowserAutofillAction.Internal) {
        when (action) {
            is SetupBrowserAutofillAction.Internal.BrowserAutofillStatusReceive -> {
                handleBrowserAutofillStatusReceive(action)
            }
        }
    }

    private fun handleBrowserIntegrationClick(
        action: SetupBrowserAutofillAction.BrowserIntegrationClick,
    ) {
        sendEvent(
            SetupBrowserAutofillEvent.NavigateToBrowserAutofillSettings(action.browserPackage),
        )
    }

    private fun handleDismissDialog() {
        mutableStateFlow.update { it.copy(dialogState = null) }
    }

    private fun handleContinueClick() {
        authRepository.setOnboardingStatus(status = OnboardingStatus.FINAL_STEP)
    }

    private fun handleTurnOnLaterClick() {
        mutableStateFlow.update {
            it.copy(dialogState = SetupBrowserAutofillState.DialogState.TurnOnLaterDialog)
        }
    }

    private fun handleTurnOnLaterConfirmClick() {
        mutableStateFlow.update { it.copy(dialogState = null) }
        authRepository.setOnboardingStatus(status = OnboardingStatus.FINAL_STEP)
    }

    private fun handleBrowserAutofillStatusReceive(
        action: SetupBrowserAutofillAction.Internal.BrowserAutofillStatusReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                browserAutofillSettingsOptions = action.status.toBrowserAutoFillSettingsOptions(),
            )
        }
    }
}

/**
 * UI State for the Setup Browser Autofill screen.
 */
@Parcelize
data class SetupBrowserAutofillState(
    val dialogState: DialogState?,
    val browserAutofillSettingsOptions: ImmutableList<BrowserAutofillSettingsOption>,
) : Parcelable {
    /**
     * Indicates if the Continue button should be enabled or not.
     */
    val isContinueEnabled: Boolean get() = browserAutofillSettingsOptions.any { it.isEnabled }

    /**
     * Models dialogs that can be shown on the Setup Browser Autofill screen.
     */
    @Parcelize
    sealed class DialogState : Parcelable {
        /**
         * Represents the turn on later dialog.
         */
        data object TurnOnLaterDialog : DialogState()
    }
}

/**
 * UI Events for the Setup Browser Autofill screen.
 */
sealed class SetupBrowserAutofillEvent {
    /**
     * Navigate to the Autofill settings of the specified [browserPackage].
     */
    data class NavigateToBrowserAutofillSettings(
        val browserPackage: BrowserPackage,
    ) : SetupBrowserAutofillEvent()
}

/**
 * UI Actions for the Setup Browser Autofill screen.
 */
sealed class SetupBrowserAutofillAction {
    /**
     * Indicates that a browser integration toggle was clicked.
     */
    data class BrowserIntegrationClick(
        val browserPackage: BrowserPackage,
    ) : SetupBrowserAutofillAction()

    /**
     * Indicates that the dialog has been dismissed.
     */
    data object DismissDialog : SetupBrowserAutofillAction()

    /**
     * Indicates that the "Continue" button was clicked.
     */
    data object ContinueClick : SetupBrowserAutofillAction()

    /**
     * Indicates that the "Turn on later" button was clicked.
     */
    data object TurnOnLaterClick : SetupBrowserAutofillAction()

    /**
     * Indicates that the confirmation button was clicked to turn on later.
     */
    data object TurnOnLaterConfirmClick : SetupBrowserAutofillAction()

    /**
     * Models actions the [SetupBrowserAutofillViewModel] itself may send.
     */
    sealed class Internal : SetupBrowserAutofillAction() {
        /**
         * Received updated [BrowserThirdPartyAutofillStatus] data.
         */
        data class BrowserAutofillStatusReceive(
            val status: BrowserThirdPartyAutofillStatus,
        ) : Internal()
    }
}
