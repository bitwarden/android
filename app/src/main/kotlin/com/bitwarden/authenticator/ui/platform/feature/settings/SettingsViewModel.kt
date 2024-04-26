package com.bitwarden.authenticator.ui.platform.feature.settings

import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.concat
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.Year
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    clock: Clock,
    private val settingsRepository: SettingsRepository,
    private val clipboardManager: BitwardenClipboardManager,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: createInitialState(
            clock,
            settingsRepository.appLanguage,
            settingsRepository.appTheme,
            settingsRepository.isUnlockWithBiometricsEnabled
        )
) {
    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SecurityClick -> {
                handleSecurityClick(action)
            }

            is SettingsAction.VaultClick -> {
                handleVaultClick(action)
            }

            is SettingsAction.AppearanceChange -> {
                handleAppearanceChange(action)
            }

            is SettingsAction.HelpClick -> {
                handleHelpClick(action)
            }

            is SettingsAction.AboutClick -> {
                handleAboutClick(action)
            }

            is SettingsAction.Internal.BiometricsKeyResultReceive -> {
                handleBiometricsKeyResultReceive(action)
            }
        }
    }

    private fun handleSecurityClick(action: SettingsAction.SecurityClick) {
        when (action) {
            is SettingsAction.SecurityClick.UnlockWithBiometricToggle -> {
                handleBiometricsSetupClick(action)
            }
        }
    }

    private fun handleBiometricsSetupClick(
        action: SettingsAction.SecurityClick.UnlockWithBiometricToggle,
    ) {
        if (action.enabled) {
            mutableStateFlow.update {
                it.copy(
                    dialog = SettingsState.Dialog.Loading(R.string.saving.asText()),
                    isUnlockWithBiometricsEnabled = true,
                )
            }
            viewModelScope.launch {
                val result = settingsRepository.setupBiometricsKey()
                sendAction(SettingsAction.Internal.BiometricsKeyResultReceive(result))
            }
        } else {
            settingsRepository.clearBiometricsKey()
            mutableStateFlow.update { it.copy(isUnlockWithBiometricsEnabled = false) }
        }
    }

    private fun handleBiometricsKeyResultReceive(
        action: SettingsAction.Internal.BiometricsKeyResultReceive,
    ) {
        when (action.result) {
            BiometricsKeyResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = false,
                    )
                }
            }

            BiometricsKeyResult.Success -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = null,
                        isUnlockWithBiometricsEnabled = true,
                    )
                }
            }
        }
    }

    private fun handleVaultClick(action: SettingsAction.VaultClick) {
        when (action) {
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
            SettingsAction.HelpClick.HelpCenterClick -> handleHelpCenterClick()
        }
    }

    private fun handleShowTutorialCLick() {
        sendEvent(SettingsEvent.NavigateToTutorial)
    }

    private fun handleHelpCenterClick() {
        sendEvent(SettingsEvent.NavigateToHelpCenter)
    }

    private fun handleAboutClick(action: SettingsAction.AboutClick) {
        when (action) {
            SettingsAction.AboutClick.PrivacyPolicyClick -> {
                handlePrivacyPolicyClick()
            }

            SettingsAction.AboutClick.VersionClick -> {
                handleVersionClick()
            }
        }
    }

    private fun handlePrivacyPolicyClick() {
        sendEvent(SettingsEvent.NavigateToPrivacyPolicy)
    }

    private fun handleVersionClick() {
        clipboardManager.setText(
            text = state.copyrightInfo.concat("\n\n".asText()).concat(state.version),
        )
    }

    companion object {
        fun createInitialState(
            clock: Clock,
            appLanguage: AppLanguage,
            appTheme: AppTheme,
            unlockWithBiometricsEnabled: Boolean,
        ): SettingsState {
            val currentYear = Year.now(clock)
            val copyrightInfo = "© Bitwarden Inc. 2015-$currentYear".asText()
            return SettingsState(
                appearance = SettingsState.Appearance(
                    language = appLanguage,
                    theme = appTheme,
                ),
                isUnlockWithBiometricsEnabled = unlockWithBiometricsEnabled,
                version = R.string.version
                    .asText()
                    .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
                copyrightInfo = copyrightInfo,
                dialog = null,
            )
        }
    }
}

/**
 * Models state of the Settings screen.
 */
@Parcelize
data class SettingsState(
    val appearance: Appearance,
    val isUnlockWithBiometricsEnabled: Boolean,
    val dialog: Dialog?,
    val version: Text,
    val copyrightInfo: Text,
) : Parcelable {

    @Parcelize
    sealed class Dialog : Parcelable {

        data class Loading(
            val message: Text,
        ) : Dialog()
    }

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

    /**
     * Navigate to the Tutorial screen.
     */
    data object NavigateToTutorial : SettingsEvent()

    /**
     * Navigate to the Export screen.
     */
    data object NavigateToExport : SettingsEvent()

    /**
     * Navigate to the Help Center web page.
     */
    data object NavigateToHelpCenter : SettingsEvent()

    /**
     * Navigate to the privacy policy web page.
     */
    data object NavigateToPrivacyPolicy : SettingsEvent()
}

/**
 * Models actions for the settings screen.
 */
sealed class SettingsAction(
    val dialog: Dialog? = null,
) {

    /**
     * Represents dialogs that may be displayed by the Settings screen.
     */
    sealed class Dialog {

        /**
         *
         */
        data class Loading(
            val message: Text,
        ) : Dialog()
    }

    sealed class SecurityClick : SettingsAction() {
        data class UnlockWithBiometricToggle(val enabled: Boolean) : SecurityClick()
    }

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

        /**
         * Indicates teh user clicked About.
         */
        data object HelpCenterClick : HelpClick()
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

    /**
     * Models actions for the About section of settings.
     */
    sealed class AboutClick : SettingsAction() {

        /**
         * Indicates the user clicked privacy policy.
         */
        data object PrivacyPolicyClick : AboutClick()

        /**
         * Indicates the user clicked version.
         */
        data object VersionClick : AboutClick()
    }

    /**
     * Models actions that the Settings screen itself may send.
     */
    sealed class Internal {

        /**
         * Indicates the biometrics key validation results has been received.
         */
        data class BiometricsKeyResultReceive(val result: BiometricsKeyResult) : SettingsAction()
    }
}
