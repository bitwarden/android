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
                // TODO: BIT-927 Launch auto-fill UI
            }

            Settings.VAULT -> {
                // TODO: BIT-928 Launch vault UI
            }

            Settings.APPEARANCE -> {
                // TODO: BIT-929 Launch appearance UI
            }

            Settings.OTHER -> {
                // TODO: BIT-930 Launch other UI
            }

            Settings.ABOUT -> {
                // TODO: BIT-931 Launch about UI
            }
        }
    }
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent {
    /**
     * Navigate to the account security screen.
     */
    data object NavigateAccountSecurity : SettingsEvent()
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
// TODO: BIT-944 Missing correct resources for "Account Security", "Vault", and "Appearance".
enum class Settings(val text: Text) {
    ACCOUNT_SECURITY(R.string.security.asText()),
    AUTO_FILL(R.string.autofill.asText()),
    VAULT(R.string.vaults.asText()),
    APPEARANCE(R.string.language.asText()),
    OTHER(R.string.other.asText()),
    ABOUT(R.string.about.asText()),
}
