package com.bitwarden.authenticator.ui.platform.feature.settings

import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
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
            ),
        ),
) {
    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.VaultClick -> handleVaultClick(action)
            is SettingsAction.AppearanceChange -> handleAppearanceChange(action)
            is SettingsAction.HelpClick -> handleHelpClick(action)
        }
    }

    private fun handleVaultClick(action: SettingsAction.VaultClick) {
        when(action) {
            SettingsAction.VaultClick.ExportClick -> handleExportClick()
        }
    }

    private fun handleExportClick() {
        sendEvent(SettingsEvent.NavigateToExport)
    }

    private fun handleAppearanceChange(action: SettingsAction.AppearanceChange) {
        when (action) {
            is SettingsAction.AppearanceChange.LanguageChange -> {
                handleLanguageChange(action.language)
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
    ) : Parcelable
}

/**
 * Models events for the settings screen.
 */
sealed class SettingsEvent {
    data object NavigateToTutorial : SettingsEvent()

    data object NavigateToExport : SettingsEvent()
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction {

    /**
     * Models actions for the Vault section of settings.
     */
    sealed class VaultClick : SettingsAction() {

        /**
         * Indicates the user clicked export.
         */
        data object ExportClick : VaultClick()
    }

    /**
     * Models actions for the Help section of settings.
     */
    sealed class HelpClick : SettingsAction() {

        /**
         * Indicates the user clicked launch tutorial.
         */
        data object ShowTutorialClick : HelpClick()
    }

    /**
     * Models actions for the Appearance section of settings.
     */
    sealed class AppearanceChange : SettingsAction() {
        /**
         * Indicates the user changed the language.
         */
        data class LanguageChange(
            val language: AppLanguage,
        ) : AppearanceChange()

        /**
         * Indicates the user selected a new theme.
         */
        data class ThemeChange(
            val appTheme: AppTheme,
        ) : AppearanceChange()
    }
}
