package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.material3.Text
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.BackgroundEvent
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
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
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = SettingsState(
        securityCount = firstTimeActionManager.allSecuritySettingsBadgeCountFlow.value,
        autoFillCount = firstTimeActionManager.allAutofillSettingsBadgeCountFlow.value,
        vaultCount = firstTimeActionManager.allVaultSettingsBadgeCountFlow.value,
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

        when (specialCircumstanceManager.specialCircumstance) {
            SpecialCircumstance.AccountSecurityShortcut -> {
                sendEvent(SettingsEvent.NavigateAccountSecurityShortcut)
                specialCircumstanceManager.specialCircumstance = null
            }

            else -> Unit
        }
    }

    override fun handleAction(action: SettingsAction): Unit = when (action) {
        is SettingsAction.SettingsClick -> handleAccountSecurityClick(action)
        is SettingsAction.Internal.SettingsNotificationCountUpdate -> {
            handleSettingsNotificationCountUpdate(action)
        }
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

    private fun handleAccountSecurityClick(action: SettingsAction.SettingsClick) {
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
    private val autoFillCount: Int,
    private val securityCount: Int,
    private val vaultCount: Int,
) {
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
     * Navigate to the about screen.
     */
    data object NavigateAbout : SettingsEvent()

    /**
     * Navigate to the account security screen.
     */
    data object NavigateAccountSecurity : SettingsEvent()

    /**
     * Navigate to the account security screen.
     */
    data object NavigateAccountSecurityShortcut : SettingsEvent(), BackgroundEvent

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
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {
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
    val testTag: String,
) {
    ACCOUNT_SECURITY(
        text = R.string.account_security.asText(),
        testTag = "AccountSecuritySettingsButton",
    ),
    AUTO_FILL(
        text = R.string.autofill.asText(),
        testTag = "AutofillSettingsButton",
    ),
    VAULT(
        text = R.string.vault.asText(),
        testTag = "VaultSettingsButton",
    ),
    APPEARANCE(
        text = R.string.appearance.asText(),
        testTag = "AppearanceSettingsButton",
    ),
    OTHER(
        text = R.string.other.asText(),
        testTag = "OtherSettingsButton",
    ),
    ABOUT(
        text = R.string.about.asText(),
        testTag = "AboutSettingsButton",
    ),
}
