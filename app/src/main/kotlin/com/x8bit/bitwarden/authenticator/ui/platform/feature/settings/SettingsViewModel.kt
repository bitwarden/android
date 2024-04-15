package com.x8bit.bitwarden.authenticator.ui.platform.feature.settings

import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import com.x8bit.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val settingsRepository: SettingsRepository,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: SettingsState(
            appearance = SettingsState.Appearance(
                language = settingsRepository.appLanguage,
                theme = settingsRepository.appTheme,
                showWebsiteIcons = !settingsRepository.isIconLoadingDisabled,
            ),
        ),
) {
    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.AppearanceChange -> handleAppearanceChange(action)
            is SettingsAction.HelpClick -> handleHelpClick(action)
        }
    }

    private fun handleAppearanceChange(action: SettingsAction.AppearanceChange) {
        when (action) {
            is SettingsAction.AppearanceChange.LanguageChange -> {
                handleLanguageChange(action.language)
            }

            is SettingsAction.AppearanceChange.ShowWebsiteIconsChange -> {
                handleShowWebsiteIconsChange(action.showWebsiteIcons)
            }

            is SettingsAction.AppearanceChange.ThemeChange -> {
                handleThemeChange(action.appTheme)
            }
        }
    }

    private fun handleLanguageChange(language: AppLanguage) {
        mutableStateFlow.update {
            it.copy(
                appearance = it.appearance.copy(language = language)
            )
        }
        settingsRepository.appLanguage = language
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(
            language.localeName,
        )
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun handleShowWebsiteIconsChange(showWebsiteIcons: Boolean) {
        mutableStateFlow.update {
            it.copy(
                appearance = it.appearance.copy(showWebsiteIcons = showWebsiteIcons)
            )
        }
        // Negate the boolean to properly update the settings repository
        settingsRepository.isIconLoadingDisabled = !showWebsiteIcons
    }

    private fun handleThemeChange(theme: AppTheme) {
        mutableStateFlow.update {
            it.copy(
                appearance = it.appearance.copy(theme = theme)
            )
        }
        settingsRepository.appTheme = theme
    }

    private fun handleHelpClick(action: SettingsAction.HelpClick) {
        when (action) {
            SettingsAction.HelpClick.ShowTutorialClick -> handleShowTutorialCLick()
        }
    }

    private fun handleShowTutorialCLick() {
        sendEvent(SettingsEvent.NavigateToTutorial)
    }
}

/**
 * Models state of the Settings screen.
 */
@Parcelize
data class SettingsState(
    val appearance: Appearance,
) : Parcelable {
    /**
     * Models state of the Appearance settings.
     */
    @Parcelize
    data class Appearance(
        val language: AppLanguage,
        val theme: AppTheme,
        val showWebsiteIcons: Boolean,
    ) : Parcelable
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent {
    data object NavigateToTutorial : SettingsEvent()
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {

    sealed class HelpClick : SettingsAction() {
        data object ShowTutorialClick : HelpClick()
    }

    sealed class AppearanceChange : SettingsAction() {
        /**
         * Indicates the user changed the language.
         */
        data class LanguageChange(
            val language: AppLanguage,
        ) : AppearanceChange()

        /**
         * Indicates the user toggled the Show Website Icons switch to [showWebsiteIcons].
         */
        data class ShowWebsiteIconsChange(
            val showWebsiteIcons: Boolean,
        ) : AppearanceChange()

        /**
         * Indicates the user selected a new theme.
         */
        data class ThemeChange(
            val appTheme: AppTheme,
        ) : AppearanceChange()
    }
}
