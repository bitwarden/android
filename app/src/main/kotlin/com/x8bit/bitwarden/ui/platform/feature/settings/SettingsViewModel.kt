package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.material3.Text
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.base.DeferredBackgroundEvent
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    specialCircumstanceManager: SpecialCircumstanceManager,
    firstTimeActionManager: FirstTimeActionManager,
    featureFlagManager: FeatureFlagManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = SettingsState(
        isPreAuth = savedStateHandle.toSettingsArgs().isPreAuth,
        securityCount = firstTimeActionManager.allSecuritySettingsBadgeCountFlow.value,
        autoFillCount = firstTimeActionManager.allAutofillSettingsBadgeCountFlow.value,
        vaultCount = firstTimeActionManager.allVaultSettingsBadgeCountFlow.value,
        isMobilePremiumUpgradeEnabled = featureFlagManager
            .getFeatureFlag(FlagKey.MobilePremiumUpgrade),
    ),
) {

    init {
        combine(
            firstTimeActionManager.allSecuritySettingsBadgeCountFlow,
            firstTimeActionManager.allAutofillSettingsBadgeCountFlow,
            firstTimeActionManager.allVaultSettingsBadgeCountFlow,
        ) { securityCount, autofillCount, vaultCount ->
            SettingsAction.Internal.SettingsNotificationCountUpdate(
                securityCount = securityCount,
                autoFillCount = autofillCount,
                vaultCount = vaultCount,
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        featureFlagManager
            .getFeatureFlagFlow(FlagKey.MobilePremiumUpgrade)
            .map {
                SettingsAction.Internal.MobilePremiumUpgradeFlagUpdate(
                    isMobilePremiumUpgradeEnabled = it,
                )
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        when (specialCircumstanceManager.specialCircumstance) {
            SpecialCircumstance.AccountSecurityShortcut -> {
                sendEvent(SettingsEvent.NavigateAccountSecurityShortcut)
                specialCircumstanceManager.specialCircumstance = null
            }

            else -> Unit
        }
    }

    override fun handleAction(action: SettingsAction): Unit = when (action) {
        is SettingsAction.CloseClick -> handleCloseClick()
        is SettingsAction.SettingsClick -> handleSettingsClick(action)
        is SettingsAction.Internal.SettingsNotificationCountUpdate -> {
            handleSettingsNotificationCountUpdate(action)
        }

        is SettingsAction.Internal.MobilePremiumUpgradeFlagUpdate -> {
            handleMobilePremiumUpgradeFlagUpdate(action)
        }
    }

    private fun handleCloseClick() {
        sendEvent(SettingsEvent.NavigateBack)
    }

    private fun handleSettingsNotificationCountUpdate(
        action: SettingsAction.Internal.SettingsNotificationCountUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                autoFillCount = action.autoFillCount,
                securityCount = action.securityCount,
                vaultCount = action.vaultCount,
            )
        }
    }

    private fun handleMobilePremiumUpgradeFlagUpdate(
        action: SettingsAction.Internal.MobilePremiumUpgradeFlagUpdate,
    ) {
        mutableStateFlow.update {
            it.copy(
                isMobilePremiumUpgradeEnabled =
                    action.isMobilePremiumUpgradeEnabled,
            )
        }
    }

    private fun handleSettingsClick(action: SettingsAction.SettingsClick) {
        when (action.settings) {
            Settings.ACCOUNT_SECURITY -> {
                sendEvent(SettingsEvent.NavigateAccountSecurity)
            }

            Settings.AUTO_FILL -> {
                sendEvent(SettingsEvent.NavigateAutoFill)
            }

            Settings.VAULT -> {
                sendEvent(SettingsEvent.NavigateVault)
            }

            Settings.APPEARANCE -> {
                sendEvent(SettingsEvent.NavigateAppearance)
            }

            Settings.PLAN -> {
                sendEvent(SettingsEvent.NavigatePlan)
            }

            Settings.OTHER -> {
                sendEvent(SettingsEvent.NavigateOther)
            }

            Settings.ABOUT -> {
                sendEvent(SettingsEvent.NavigateAbout)
            }
        }
    }
}

/**
 * Models the state of the settings screen.
 */
data class SettingsState(
    private val isPreAuth: Boolean,
    private val autoFillCount: Int,
    private val securityCount: Int,
    private val vaultCount: Int,
    private val isMobilePremiumUpgradeEnabled: Boolean = false,
) {
    val shouldShowCloseButton: Boolean = isPreAuth

    /**
     * Whether the plan row should be shown. The row is visible when the
     * mobile premium upgrade feature flag is enabled and the user is
     * authenticated.
     */
    private val shouldShowPlanRow: Boolean =
        !isPreAuth && isMobilePremiumUpgradeEnabled

    val settingRows: ImmutableList<Settings> = Settings
        .entries
        .filter { setting ->
            when (setting) {
                Settings.ACCOUNT_SECURITY -> !isPreAuth
                Settings.AUTO_FILL -> !isPreAuth
                Settings.VAULT -> !isPreAuth
                Settings.APPEARANCE -> true
                Settings.PLAN -> shouldShowPlanRow
                Settings.OTHER -> true
                Settings.ABOUT -> true
            }
        }
        .toImmutableList()

    val notificationBadgeCountMap: Map<Settings, Int> = mapOf(
        Settings.ACCOUNT_SECURITY to securityCount,
        Settings.AUTO_FILL to autoFillCount,
        Settings.VAULT to vaultCount,
    )
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent {
    /**
     * Navigates back. This is only possible prior to login.
     */
    data object NavigateBack : SettingsEvent()

    /**
     * Navigate to the about screen.
     */
    data object NavigateAbout : SettingsEvent()

    /**
     * Navigate to the account security screen.
     */
    data object NavigateAccountSecurity : SettingsEvent()

    /**
     * Navigate to the account security screen via shortcut.
     */
    data object NavigateAccountSecurityShortcut : SettingsEvent(), DeferredBackgroundEvent

    /**
     * Navigate to the appearance screen.
     */
    data object NavigateAppearance : SettingsEvent()

    /**
     * Navigate to the auto-fill screen.
     */
    data object NavigateAutoFill : SettingsEvent()

    /**
     * Navigate to the other screen.
     */
    data object NavigateOther : SettingsEvent()

    /**
     * Navigate to the vault screen.
     */
    data object NavigateVault : SettingsEvent()

    /**
     * Navigate to the plan screen.
     */
    data object NavigatePlan : SettingsEvent()
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {
    /**
     * The user has clicked the close button.
     */
    data object CloseClick : SettingsAction()

    /**
     * User clicked a settings row.
     */
    data class SettingsClick(
        val settings: Settings,
    ) : SettingsAction()

    /**
     * Models internal actions for the settings screen.
     */
    sealed class Internal : SettingsAction() {
        /**
         * Update the notification count for the settings rows.
         */
        data class SettingsNotificationCountUpdate(
            val autoFillCount: Int,
            val securityCount: Int,
            val vaultCount: Int,
        ) : Internal()

        /**
         * Update the mobile premium upgrade feature flag state.
         */
        data class MobilePremiumUpgradeFlagUpdate(
            val isMobilePremiumUpgradeEnabled: Boolean,
        ) : Internal()
    }
}

/**
 * Enum representing the settings rows, such as "account security" or "vault".
 *
 * @property text The [Text] of the string that represents the label of each setting.
 * @property testTag The value that should be set for the test tag. This is used in Appium testing.
 */
enum class Settings(
    val text: Text,
    @field:DrawableRes val vectorIconRes: Int,
    val testTag: String,
) {
    ACCOUNT_SECURITY(
        text = BitwardenString.account_security.asText(),
        vectorIconRes = BitwardenDrawable.ic_locked,
        testTag = "AccountSecuritySettingsButton",
    ),
    AUTO_FILL(
        text = BitwardenString.autofill_noun.asText(),
        vectorIconRes = BitwardenDrawable.ic_check_mark,
        testTag = "AutofillSettingsButton",
    ),
    VAULT(
        text = BitwardenString.vault.asText(),
        vectorIconRes = BitwardenDrawable.ic_vault_thin,
        testTag = "VaultSettingsButton",
    ),
    APPEARANCE(
        text = BitwardenString.appearance.asText(),
        vectorIconRes = BitwardenDrawable.ic_paintbrush,
        testTag = "AppearanceSettingsButton",
    ),
    PLAN(
        text = BitwardenString.plan.asText(),
        vectorIconRes = BitwardenDrawable.ic_plan,
        testTag = "PlanSettingsButton",
    ),
    OTHER(
        text = BitwardenString.other.asText(),
        vectorIconRes = BitwardenDrawable.ic_filter,
        testTag = "OtherSettingsButton",
    ),
    ABOUT(
        text = BitwardenString.about.asText(),
        vectorIconRes = BitwardenDrawable.ic_info_circle,
        testTag = "AboutSettingsButton",
    ),
}
