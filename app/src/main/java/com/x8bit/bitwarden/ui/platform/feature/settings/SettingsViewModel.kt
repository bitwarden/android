package com.x8bit.bitwarden.ui.platform.feature.settings

import androidx.compose.material3.Text
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : BaseViewModel<Unit, SettingsEvent, SettingsAction>(
    initialState = Unit,
) {
    override fun handleAction(action: SettingsAction): Unit = when (action) {
        is SettingsAction.SettingsClick -> handleAccountSecurityClick(action)
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
