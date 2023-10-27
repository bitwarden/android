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
 */
enum class Settings(val text: Text) {
    ACCOUNT_SECURITY(R.string.account_security.asText()),
    AUTO_FILL(R.string.autofill.asText()),
    VAULT(R.string.vault.asText()),
    APPEARANCE(R.string.appearance.asText()),
    OTHER(R.string.other.asText()),
    ABOUT(R.string.about.asText()),
}
