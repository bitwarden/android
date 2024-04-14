package com.x8bit.bitwarden.authenticator.ui.platform.feature.settings

import androidx.compose.material3.Text
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
) : BaseViewModel<Unit, SettingsEvent, SettingsAction>(
    initialState = Unit
) {
    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SettingsClick -> handleSettingClick(action)
        }
    }

    private fun handleSettingClick(action: SettingsAction.SettingsClick) {
        when (action.setting) {
            else -> {}
        }
    }
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {
    /**
     * User clicked a settings row.
     */
    class SettingsClick(val setting: Settings) : SettingsAction()
}

/**
 * Enum representing the settings rows, such as "import" or "export".
 *
 * @property text The [Text] of the string that represents the label of each setting.
 * @property testTag The value that should be set for the test tag. This is used in Appium testing.
 */
enum class Settings(
    val text: Text,
    val testTag: String,
)
