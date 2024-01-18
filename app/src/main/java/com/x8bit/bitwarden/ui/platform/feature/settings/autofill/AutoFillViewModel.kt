package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the auto-fill screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class AutoFillViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : BaseViewModel<AutoFillState, AutoFillEvent, AutoFillAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: AutoFillState(
            isAskToAddLoginEnabled = false,
            isAutoFillServicesEnabled = settingsRepository.isAutofillEnabledStateFlow.value,
            isCopyTotpAutomaticallyEnabled = false,
            isUseInlineAutoFillEnabled = settingsRepository.isInlineAutofillEnabled,
            uriDetectionMethod = AutoFillState.UriDetectionMethod.DEFAULT,
        ),
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        settingsRepository
            .isAutofillEnabledStateFlow
            .map {
                AutoFillAction.Internal.AutofillEnabledUpdateReceive(isAutofillEnabled = it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AutoFillAction): Unit = when (action) {
        is AutoFillAction.AskToAddLoginClick -> handleAskToAddLoginClick(action)
        is AutoFillAction.AutoFillServicesClick -> handleAutoFillServicesClick(action)
        AutoFillAction.BackClick -> handleBackClick()
        is AutoFillAction.CopyTotpAutomaticallyClick -> handleCopyTotpAutomaticallyClick(action)
        is AutoFillAction.UriDetectionMethodSelect -> handleUriDetectionMethodSelect(action)
        is AutoFillAction.UseInlineAutofillClick -> handleUseInlineAutofillClick(action)
        is AutoFillAction.Internal.AutofillEnabledUpdateReceive -> {
            handleAutofillEnabledUpdateReceive(action)
        }
    }

    private fun handleAskToAddLoginClick(action: AutoFillAction.AskToAddLoginClick) {
        // TODO BIT-1092: Persist selection
        sendEvent(AutoFillEvent.ShowToast("Not yet implemented.".asText()))
        mutableStateFlow.update { it.copy(isAskToAddLoginEnabled = action.isEnabled) }
    }

    private fun handleAutoFillServicesClick(action: AutoFillAction.AutoFillServicesClick) {
        if (action.isEnabled) {
            sendEvent(AutoFillEvent.NavigateToAutofillSettings)
        } else {
            settingsRepository.disableAutofill()
        }
    }

    private fun handleBackClick() {
        sendEvent(AutoFillEvent.NavigateBack)
    }

    private fun handleCopyTotpAutomaticallyClick(
        action: AutoFillAction.CopyTotpAutomaticallyClick,
    ) {
        // TODO BIT-1093: Persist selection
        sendEvent(AutoFillEvent.ShowToast("Not yet implemented.".asText()))
        mutableStateFlow.update { it.copy(isCopyTotpAutomaticallyEnabled = action.isEnabled) }
    }

    private fun handleUseInlineAutofillClick(action: AutoFillAction.UseInlineAutofillClick) {
        settingsRepository.isInlineAutofillEnabled = action.isEnabled
        mutableStateFlow.update { it.copy(isUseInlineAutoFillEnabled = action.isEnabled) }
    }

    private fun handleUriDetectionMethodSelect(action: AutoFillAction.UriDetectionMethodSelect) {
        // TODO BIT-1094: Persist selection
        sendEvent(AutoFillEvent.ShowToast("Not yet implemented.".asText()))
        mutableStateFlow.update {
            it.copy(uriDetectionMethod = action.uriDetectionMethod)
        }
    }

    private fun handleAutofillEnabledUpdateReceive(
        action: AutoFillAction.Internal.AutofillEnabledUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isAutoFillServicesEnabled = action.isAutofillEnabled)
        }
    }
}

/**
 * Models state for the Auto-fill screen.
 */
@Parcelize
data class AutoFillState(
    val isAskToAddLoginEnabled: Boolean,
    val isAutoFillServicesEnabled: Boolean,
    val isCopyTotpAutomaticallyEnabled: Boolean,
    val isUseInlineAutoFillEnabled: Boolean,
    val uriDetectionMethod: UriDetectionMethod,
) : Parcelable {
    /**
     * A representation of the URI detection methods.
     */
    enum class UriDetectionMethod(val text: Text) {
        DEFAULT(text = R.string.default_text.asText()),
        BASE_DOMAIN(text = R.string.base_domain.asText()),
        HOST(text = R.string.host.asText()),
        STARTS_WITH(text = R.string.starts_with.asText()),
        REGULAR_EXPRESSION(text = R.string.reg_ex.asText()),
        EXACT(text = R.string.exact.asText()),
        NEVER(text = R.string.never.asText()),
    }
}

/**
 * Models events for the auto-fill screen.
 */
sealed class AutoFillEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AutoFillEvent()

    /**
     * Navigates to the system autofill settings selection screen.
     */
    data object NavigateToAutofillSettings : AutoFillEvent()

    /**
     * Displays a toast with the given [Text].
     */
    data class ShowToast(
        val text: Text,
    ) : AutoFillEvent()
}

/**
 * Models actions for the auto-fill screen.
 */
sealed class AutoFillAction {
    /**
     * User clicked ask to add login button.
     */
    data class AskToAddLoginClick(
        val isEnabled: Boolean,
    ) : AutoFillAction()

    /**
     * User clicked auto-fill services button.
     */
    data class AutoFillServicesClick(
        val isEnabled: Boolean,
    ) : AutoFillAction()

    /**
     * User clicked back button.
     */
    data object BackClick : AutoFillAction()

    /**
     * User clicked copy TOTP automatically button.
     */
    data class CopyTotpAutomaticallyClick(
        val isEnabled: Boolean,
    ) : AutoFillAction()

    /**
     * User selected a [AutoFillState.UriDetectionMethod].
     */
    data class UriDetectionMethodSelect(
        val uriDetectionMethod: AutoFillState.UriDetectionMethod,
    ) : AutoFillAction()

    /**
     * User clicked use inline autofill button.
     */
    data class UseInlineAutofillClick(
        val isEnabled: Boolean,
    ) : AutoFillAction()

    /**
     * Internal actions.
     */
    sealed class Internal : AutoFillAction() {

        /**
         * An update for changes in the [isAutofillEnabled] value.
         */
        data class AutofillEnabledUpdateReceive(
            val isAutofillEnabled: Boolean,
        ) : Internal()
    }
}
