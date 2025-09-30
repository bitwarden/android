package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import android.os.Build
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
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
 * View model for the auto-fill screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class AutoFillViewModel @Inject constructor(
    authRepository: AuthRepository,
    browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val firstTimeActionManager: FirstTimeActionManager,
) : BaseViewModel<AutoFillState, AutoFillEvent, AutoFillAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: run {
            val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
            val firstTimeState = firstTimeActionManager.currentOrDefaultUserFirstTimeState
            AutoFillState(
                isAskToAddLoginEnabled = !settingsRepository.isAutofillSavePromptDisabled,
                isAccessibilityAutofillEnabled = settingsRepository
                    .isAccessibilityEnabledStateFlow
                    .value,
                isAutoFillServicesEnabled = settingsRepository.isAutofillEnabledStateFlow.value,
                isCopyTotpAutomaticallyEnabled = !settingsRepository.isAutoCopyTotpDisabled,
                autofillStyle = if (settingsRepository.isInlineAutofillEnabled) {
                    AutofillStyle.INLINE
                } else {
                    AutofillStyle.POPUP
                },
                showInlineAutofillOption = isBuildVersionAtLeast(Build.VERSION_CODES.R),
                showPasskeyManagementRow = isBuildVersionAtLeast(
                    Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                ),
                defaultUriMatchType = settingsRepository.defaultUriMatchType,
                showAutofillActionCard = firstTimeState.showSetupAutofillCard,
                showBrowserAutofillActionCard = firstTimeState.showSetupBrowserAutofillCard,
                activeUserId = userId,
                browserAutofillSettingsOptions = browserThirdPartyAutofillEnabledManager
                    .browserThirdPartyAutofillStatus
                    .toBrowserAutoFillSettingsOptions(),
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
            .map { AutoFillAction.Internal.UpdateShowAutofillActionCard(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatusFlow
            .map { AutoFillAction.Internal.BrowserAutofillStatusReceive(status = it) }
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
        is AutoFillAction.AutofillStyleSelected -> handleAutofillStyleSelected(action)
        AutoFillAction.PasskeyManagementClick -> handlePasskeyManagementClick()
        is AutoFillAction.Internal -> handleInternalAction(action)
        AutoFillAction.AutofillActionCardCtaClick -> handleAutofillActionCardCtaClick()
        AutoFillAction.DismissShowAutofillActionCard -> handleDismissShowAutofillActionCard()
        AutoFillAction.BrowserAutofillActionCardCtaClick -> {
            handleBrowserAutofillActionCardCtaClick()
        }

        AutoFillAction.DismissShowBrowserAutofillActionCard -> {
            handleDismissShowBrowserAutofillActionCard()
        }

        is AutoFillAction.BrowserAutofillSelected -> handleBrowserAutofillSelected(action)
        AutoFillAction.AboutPrivilegedAppsClick -> handleAboutPrivilegedAppsClick()
        AutoFillAction.PrivilegedAppsClick -> handlePrivilegedAppsClick()
        AutoFillAction.LearnMoreClick -> handleLearnMoreClick()
        AutoFillAction.HelpCardClick -> handleHelpCardClick()
    }

    private fun handlePrivilegedAppsClick() {
        sendEvent(AutoFillEvent.NavigateToPrivilegedAppsListScreen)
    }

    private fun handleLearnMoreClick() {
        sendEvent(AutoFillEvent.NavigateToLearnMore)
    }

    private fun handleHelpCardClick() {
        sendEvent(AutoFillEvent.NavigateToAutofillHelp)
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

            is AutoFillAction.Internal.BrowserAutofillStatusReceive -> {
                handleBrowserAutofillStatusReceive(action)
            }
        }
    }

    private fun handleAboutPrivilegedAppsClick() {
        sendEvent(AutoFillEvent.NavigateToAboutPrivilegedAppsScreen)
    }

    private fun handleBrowserAutofillStatusReceive(
        action: AutoFillAction.Internal.BrowserAutofillStatusReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                browserAutofillSettingsOptions = action
                    .status
                    .toBrowserAutoFillSettingsOptions(),
            )
        }
    }

    private fun handleBrowserAutofillSelected(action: AutoFillAction.BrowserAutofillSelected) {
        sendEvent(AutoFillEvent.NavigateToBrowserAutofillSettings(action.browserPackage))
    }

    private fun handleDismissShowAutofillActionCard() {
        dismissShowAutofillActionCard()
    }

    private fun handleAutofillActionCardCtaClick() {
        sendEvent(AutoFillEvent.NavigateToSetupAutofill)
    }

    private fun handleDismissShowBrowserAutofillActionCard() {
        firstTimeActionManager.storeShowBrowserAutofillSettingBadge(showBadge = false)
    }

    private fun handleBrowserAutofillActionCardCtaClick() {
        sendEvent(AutoFillEvent.NavigateToSetupBrowserAutofill)
    }

    private fun handleUpdateShowAutofillActionCard(
        action: AutoFillAction.Internal.UpdateShowAutofillActionCard,
    ) {
        mutableStateFlow.update {
            it.copy(
                showAutofillActionCard = action.firstTimeState.showSetupAutofillCard,
                showBrowserAutofillActionCard = action.firstTimeState.showSetupBrowserAutofillCard,
            )
        }
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

    private fun handleAutofillStyleSelected(action: AutoFillAction.AutofillStyleSelected) {
        settingsRepository.isInlineAutofillEnabled = action.style == AutofillStyle.INLINE
        mutableStateFlow.update { it.copy(autofillStyle = action.style) }
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
    val autofillStyle: AutofillStyle,
    val showInlineAutofillOption: Boolean,
    val showPasskeyManagementRow: Boolean,
    val defaultUriMatchType: UriMatchType,
    val showAutofillActionCard: Boolean,
    val showBrowserAutofillActionCard: Boolean,
    val activeUserId: String,
    val browserAutofillSettingsOptions: ImmutableList<BrowserAutofillSettingsOption>,
) : Parcelable {
    /**
     * Indicates which call-to-action that should be displayed.
     */
    val ctaState: CtaState
        get() = when {
            showAutofillActionCard -> CtaState.AUTOFILL
            showBrowserAutofillActionCard -> CtaState.BROWSER_AUTOFILL
            else -> CtaState.DEFAULT
        }

    /**
     * Whether or not the dropdown controlling the [autofillStyle] value is displayed.
     */
    val showInlineAutofill: Boolean get() = isAutoFillServicesEnabled && showInlineAutofillOption

    /**
     * The number of browsers that can be configured.
     */
    val browserCount: Int get() = browserAutofillSettingsOptions.size

    /**
     * Whether or not the toggles for enabling 3rd-party autofill support should be displayed.
     */
    val showBrowserSettingOptions: Boolean
        get() = isAutoFillServicesEnabled && browserAutofillSettingsOptions.isNotEmpty()
}

/**
 * A representation of which call-to-action that should be displayed.
 */
enum class CtaState {
    AUTOFILL,
    BROWSER_AUTOFILL,
    DEFAULT,
}

/**
 * The visual style of autofill that should be used.
 */
enum class AutofillStyle(val label: Text) {
    /**
     * Displays the autofill data in the keyboard.
     */
    INLINE(label = BitwardenString.autofill_suggestions_inline.asText()),

    /**
     * Displays the autofill data as a popup attached to the field you are filling.
     */
    POPUP(label = BitwardenString.autofill_suggestions_popup.asText()),
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
     * Navigate to the Autofill settings of the specified [browserPackage].
     */
    data class NavigateToBrowserAutofillSettings(
        val browserPackage: BrowserPackage,
    ) : AutoFillEvent()

    /**
     * Navigates to the setup autofill screen.
     */
    data object NavigateToSetupAutofill : AutoFillEvent()

    /**
     * Navigates to the setup browser autofill screen.
     */
    data object NavigateToSetupBrowserAutofill : AutoFillEvent()

    /**
     * Navigate to the about privileged apps screen.
     */
    data object NavigateToAboutPrivilegedAppsScreen : AutoFillEvent()

    /**
     * Navigate to the privileged apps list screen.
     */
    data object NavigateToPrivilegedAppsListScreen : AutoFillEvent()

    /**
     * Navigate to the learn more.
     */
    data object NavigateToLearnMore : AutoFillEvent()

    /**
     * Navigate to the autofill help page.
     */
    data object NavigateToAutofillHelp : AutoFillEvent()
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
    data class AutofillStyleSelected(
        val style: AutofillStyle,
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
    data object AutofillActionCardCtaClick : AutoFillAction()

    /**
     * User has clicked the "X" to dismiss the browser autofill action card.
     */
    data object DismissShowBrowserAutofillActionCard : AutoFillAction()

    /**
     * User has clicked the CTA on the browser autofill action card.
     */
    data object BrowserAutofillActionCardCtaClick : AutoFillAction()

    /**
     * User has clicked one of the browser autofill options.
     */
    data class BrowserAutofillSelected(val browserPackage: BrowserPackage) : AutoFillAction()

    /**
     * User has clicked the about privileged apps help link.
     */
    data object AboutPrivilegedAppsClick : AutoFillAction()

    /**
     * User has clicked the privileged apps row.
     */
    data object PrivilegedAppsClick : AutoFillAction()

    /**
     * User has clicked the learn more help link.
     */
    data object LearnMoreClick : AutoFillAction()

    /**
     * User has clicked the help CTA.
     */
    data object HelpCardClick : AutoFillAction()

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
         * An update for changes in the [firstTimeState] value from the settings repository.
         */
        data class UpdateShowAutofillActionCard(val firstTimeState: FirstTimeState) : Internal()

        /**
         * Received updated [BrowserThirdPartyAutofillStatus] data.
         */
        data class BrowserAutofillStatusReceive(
            val status: BrowserThirdPartyAutofillStatus,
        ) : Internal()
    }
}
