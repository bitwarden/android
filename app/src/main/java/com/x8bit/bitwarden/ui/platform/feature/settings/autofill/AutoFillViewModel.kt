package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.os.Build
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
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
    authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<AutoFillState, AutoFillEvent, AutoFillAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
            AutoFillState(
                isAskToAddLoginEnabled = !settingsRepository.isAutofillSavePromptDisabled,
                isAccessibilityAutofillEnabled = settingsRepository
                    .isAccessibilityEnabledStateFlow
                    .value,
                isAutoFillServicesEnabled = settingsRepository.isAutofillEnabledStateFlow.value,
                isCopyTotpAutomaticallyEnabled = !settingsRepository.isAutoCopyTotpDisabled,
                isUseInlineAutoFillEnabled = settingsRepository.isInlineAutofillEnabled,
                showInlineAutofillOption = !isBuildVersionBelow(Build.VERSION_CODES.R),
                showPasskeyManagementRow = !isBuildVersionBelow(
                    Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                ),
                defaultUriMatchType = settingsRepository.defaultUriMatchType,
                showAutofillActionCard = false,
                activeUserId = userId,
            )
        },
) {

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        settingsRepository
            .isAccessibilityEnabledStateFlow
            .map {
                AutoFillAction.Internal.AccessibilityEnabledUpdateReceive(
                    isAccessibilityEnabled = it,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
        settingsRepository
            .isAutofillEnabledStateFlow
            .map {
                AutoFillAction.Internal.AutofillEnabledUpdateReceive(isAutofillEnabled = it)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        firstTimeActionManager
            .firstTimeStateFlow
            .map { AutoFillAction.Internal.UpdateShowAutofillActionCard(it.showSetupAutofillCard) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AutoFillAction) = when (action) {
        is AutoFillAction.AskToAddLoginClick -> handleAskToAddLoginClick(action)
        is AutoFillAction.AutoFillServicesClick -> handleAutoFillServicesClick(action)
        AutoFillAction.BackClick -> handleBackClick()
        is AutoFillAction.CopyTotpAutomaticallyClick -> handleCopyTotpAutomaticallyClick(action)
        is AutoFillAction.DefaultUriMatchTypeSelect -> handleDefaultUriMatchTypeSelect(action)
        AutoFillAction.BlockAutoFillClick -> handleBlockAutoFillClick()
        AutoFillAction.UseAccessibilityAutofillClick -> handleUseAccessibilityAutofillClick()
        is AutoFillAction.UseInlineAutofillClick -> handleUseInlineAutofillClick(action)
        AutoFillAction.PasskeyManagementClick -> handlePasskeyManagementClick()
        is AutoFillAction.Internal -> handleInternalAction(action)
        AutoFillAction.AutoFillActionCardCtaClick -> handleAutoFillActionCardCtClick()
        AutoFillAction.DismissShowAutofillActionCard -> handleDismissShowAutofillActionCard()
    }

    private fun handleInternalAction(action: AutoFillAction.Internal) {
        when (action) {
            is AutoFillAction.Internal.AccessibilityEnabledUpdateReceive -> {
                handleAccessibilityEnabledUpdateReceive(action)
            }

            is AutoFillAction.Internal.AutofillEnabledUpdateReceive -> {
                handleAutofillEnabledUpdateReceive(action)
            }

            is AutoFillAction.Internal.UpdateShowAutofillActionCard -> {
                handleUpdateShowAutofillActionCard(action)
            }
        }
    }

    private fun handleDismissShowAutofillActionCard() {
        dismissShowAutofillActionCard()
    }

    private fun handleAutoFillActionCardCtClick() {
        sendEvent(AutoFillEvent.NavigateToSetupAutofill)
    }

    private fun handleUpdateShowAutofillActionCard(
        action: AutoFillAction.Internal.UpdateShowAutofillActionCard,
    ) {
        mutableStateFlow.update { it.copy(showAutofillActionCard = action.showAutofillActionCard) }
    }

    private fun handleAskToAddLoginClick(action: AutoFillAction.AskToAddLoginClick) {
        settingsRepository.isAutofillSavePromptDisabled = !action.isEnabled
        mutableStateFlow.update { it.copy(isAskToAddLoginEnabled = action.isEnabled) }
    }

    private fun handleAutoFillServicesClick(action: AutoFillAction.AutoFillServicesClick) {
        if (action.isEnabled) {
            sendEvent(AutoFillEvent.NavigateToAutofillSettings)
        } else {
            settingsRepository.disableAutofill()
        }
        dismissShowAutofillActionCard()
    }

    private fun handleBackClick() {
        sendEvent(AutoFillEvent.NavigateBack)
    }

    private fun handleCopyTotpAutomaticallyClick(
        action: AutoFillAction.CopyTotpAutomaticallyClick,
    ) {
        settingsRepository.isAutoCopyTotpDisabled = !action.isEnabled
        mutableStateFlow.update { it.copy(isCopyTotpAutomaticallyEnabled = action.isEnabled) }
    }

    private fun handleUseAccessibilityAutofillClick() {
        sendEvent(AutoFillEvent.NavigateToAccessibilitySettings)
    }

    private fun handleUseInlineAutofillClick(action: AutoFillAction.UseInlineAutofillClick) {
        settingsRepository.isInlineAutofillEnabled = action.isEnabled
        mutableStateFlow.update { it.copy(isUseInlineAutoFillEnabled = action.isEnabled) }
    }

    private fun handlePasskeyManagementClick() {
        sendEvent(AutoFillEvent.NavigateToSettings)
    }

    private fun handleDefaultUriMatchTypeSelect(action: AutoFillAction.DefaultUriMatchTypeSelect) {
        settingsRepository.defaultUriMatchType = action.defaultUriMatchType
        mutableStateFlow.update {
            it.copy(defaultUriMatchType = action.defaultUriMatchType)
        }
    }

    private fun handleAccessibilityEnabledUpdateReceive(
        action: AutoFillAction.Internal.AccessibilityEnabledUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isAccessibilityAutofillEnabled = action.isAccessibilityEnabled)
        }
    }

    private fun handleAutofillEnabledUpdateReceive(
        action: AutoFillAction.Internal.AutofillEnabledUpdateReceive,
    ) {
        mutableStateFlow.update {
            it.copy(isAutoFillServicesEnabled = action.isAutofillEnabled)
        }
    }

    private fun handleBlockAutoFillClick() {
        sendEvent(AutoFillEvent.NavigateToBlockAutoFill)
    }

    private fun dismissShowAutofillActionCard() {
        if (!state.showAutofillActionCard) return
        firstTimeActionManager.storeShowAutoFillSettingBadge(
            showBadge = false,
        )
    }
}

/**
 * Models state for the Auto-fill screen.
 */
@Parcelize
data class AutoFillState(
    val isAskToAddLoginEnabled: Boolean,
    val isAccessibilityAutofillEnabled: Boolean,
    val isAutoFillServicesEnabled: Boolean,
    val isCopyTotpAutomaticallyEnabled: Boolean,
    val isUseInlineAutoFillEnabled: Boolean,
    val showInlineAutofillOption: Boolean,
    val showPasskeyManagementRow: Boolean,
    val defaultUriMatchType: UriMatchType,
    val showAutofillActionCard: Boolean,
    val activeUserId: String,
) : Parcelable {

    /**
     * Whether or not the toggle controlling the [isUseInlineAutoFillEnabled] value can be
     * interacted with.
     */
    val canInteractWithInlineAutofillToggle: Boolean
        get() = isAutoFillServicesEnabled
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
     * Navigates to the system accessibility settings selection screen.
     */
    data object NavigateToAccessibilitySettings : AutoFillEvent()

    /**
     * Navigates to the system autofill settings selection screen.
     */
    data object NavigateToAutofillSettings : AutoFillEvent()

    /**
     * Navigate to block auto fill screen.
     */
    data object NavigateToBlockAutoFill : AutoFillEvent()

    /**
     * Navigate to device settings.
     */
    data object NavigateToSettings : AutoFillEvent()

    /**
     * Displays a toast with the given [Text].
     */
    data class ShowToast(
        val text: Text,
    ) : AutoFillEvent()

    /**
     * Navigates to the setup autofill screen.
     */
    data object NavigateToSetupAutofill : AutoFillEvent()
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
     * User selected a [UriMatchType].
     */
    data class DefaultUriMatchTypeSelect(
        val defaultUriMatchType: UriMatchType,
    ) : AutoFillAction()

    /**
     * User clicked block auto fill button.
     */
    data object BlockAutoFillClick : AutoFillAction()

    /**
     * User clicked use accessibility autofill switch.
     */
    data object UseAccessibilityAutofillClick : AutoFillAction()

    /**
     * User clicked use inline autofill button.
     */
    data class UseInlineAutofillClick(
        val isEnabled: Boolean,
    ) : AutoFillAction()

    /**
     * User clicked passkey management button.
     */
    data object PasskeyManagementClick : AutoFillAction()

    /**
     * User has clicked the "X" to dismiss the autofill action card.
     */
    data object DismissShowAutofillActionCard : AutoFillAction()

    /**
     * User has clicked the CTA on the autofill action card.
     */
    data object AutoFillActionCardCtaClick : AutoFillAction()

    /**
     * Internal actions.
     */
    sealed class Internal : AutoFillAction() {
        /**
         * An update for changes in the [isAccessibilityEnabled] value.
         */
        data class AccessibilityEnabledUpdateReceive(
            val isAccessibilityEnabled: Boolean,
        ) : Internal()

        /**
         * An update for changes in the [isAutofillEnabled] value.
         */
        data class AutofillEnabledUpdateReceive(
            val isAutofillEnabled: Boolean,
        ) : Internal()

        /**
         * An update for changes in the [showAutofillActionCard] value from the settings repository.
         */
        data class UpdateShowAutofillActionCard(val showAutofillActionCard: Boolean) : Internal()
    }
}
