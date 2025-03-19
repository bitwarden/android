package com.bitwarden.authenticator.ui.platform.feature.settings

import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.util.isSyncWithBitwardenEnabled
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.base.util.concat
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    clock: Clock,
    private val authenticatorRepository: AuthenticatorRepository,
    private val authenticatorBridgeManager: AuthenticatorBridgeManager,
    private val settingsRepository: SettingsRepository,
    private val clipboardManager: BitwardenClipboardManager,
    featureFlagManager: FeatureFlagManager,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: createInitialState(
            clock = clock,
            appLanguage = settingsRepository.appLanguage,
            appTheme = settingsRepository.appTheme,
            unlockWithBiometricsEnabled = settingsRepository.isUnlockWithBiometricsEnabled,
            isSubmitCrashLogsEnabled = settingsRepository.isCrashLoggingEnabled,
            isSyncWithBitwardenFeatureEnabled =
            featureFlagManager.getFeatureFlag(FlagKey.PasswordManagerSync),
            accountSyncState = authenticatorBridgeManager.accountSyncStateFlow.value,
            defaultSaveOption = settingsRepository.defaultSaveOption,
            sharedAccountsState = authenticatorRepository.sharedCodesStateFlow.value,
        ),
) {

    init {
        authenticatorRepository
            .sharedCodesStateFlow
            .map { SettingsAction.Internal.SharedAccountsStateUpdated(it) }
            .onEach(::handleAction)
            .launchIn(viewModelScope)

        settingsRepository
            .defaultSaveOptionFlow
            .map { SettingsAction.Internal.DefaultSaveOptionUpdated(it) }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SecurityClick -> {
                handleSecurityClick(action)
            }

            is SettingsAction.DataClick -> {
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

            is SettingsAction.Internal.SharedAccountsStateUpdated -> {
                handleSharedAccountsStateUpdated(action)
            }

            is SettingsAction.Internal.DefaultSaveOptionUpdated -> {
                handleDefaultSaveOptionUpdated(action)
            }
        }
    }

    private fun handleSharedAccountsStateUpdated(
        action: SettingsAction.Internal.SharedAccountsStateUpdated,
    ) {
        mutableStateFlow.update {
            it.copy(
                showDefaultSaveOptionRow = action.state.isSyncWithBitwardenEnabled,
            )
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

    private fun handleVaultClick(action: SettingsAction.DataClick) {
        when (action) {
            SettingsAction.DataClick.ExportClick -> handleExportClick()
            SettingsAction.DataClick.ImportClick -> handleImportClick()
            SettingsAction.DataClick.BackupClick -> handleBackupClick()
            SettingsAction.DataClick.SyncWithBitwardenClick -> handleSyncWithBitwardenClick()
            is SettingsAction.DataClick.DefaultSaveOptionUpdated ->
                handleDefaultSaveOptionChosen(action)
        }
    }

    private fun handleDefaultSaveOptionChosen(
        action: SettingsAction.DataClick.DefaultSaveOptionUpdated,
    ) {
        settingsRepository.defaultSaveOption = action.option
    }

    private fun handleDefaultSaveOptionUpdated(
        action: SettingsAction.Internal.DefaultSaveOptionUpdated,
    ) {
        mutableStateFlow.update {
            it.copy(
                defaultSaveOption = action.option,
            )
        }
    }

    private fun handleSyncWithBitwardenClick() {
        when (authenticatorBridgeManager.accountSyncStateFlow.value) {
            AccountSyncState.AppNotInstalled -> {
                sendEvent(SettingsEvent.NavigateToBitwardenPlayStoreListing)
            }

            else -> sendEvent(SettingsEvent.NavigateToBitwardenApp)
        }
    }

    private fun handleExportClick() {
        sendEvent(SettingsEvent.NavigateToExport)
    }

    private fun handleImportClick() {
        sendEvent(SettingsEvent.NavigateToImport)
    }

    private fun handleBackupClick() {
        sendEvent(SettingsEvent.NavigateToBackup)
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
                appearance = it.appearance.copy(language = language),
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
                appearance = it.appearance.copy(theme = theme),
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

            is SettingsAction.AboutClick.SubmitCrashLogsClick -> {
                handleSubmitCrashLogsClick(action.enabled)
            }
        }
    }

    private fun handleSubmitCrashLogsClick(enabled: Boolean) {
        mutableStateFlow.update { it.copy(isSubmitCrashLogsEnabled = enabled) }
        settingsRepository.isCrashLoggingEnabled = enabled
    }

    private fun handlePrivacyPolicyClick() {
        sendEvent(SettingsEvent.NavigateToPrivacyPolicy)
    }

    private fun handleVersionClick() {
        clipboardManager.setText(
            text = state.copyrightInfo.concat("\n\n".asText()).concat(state.version),
        )
    }

    @Suppress("UndocumentedPublicClass")
    companion object {
        @Suppress("LongParameterList")
        private fun createInitialState(
            clock: Clock,
            appLanguage: AppLanguage,
            appTheme: AppTheme,
            defaultSaveOption: DefaultSaveOption,
            unlockWithBiometricsEnabled: Boolean,
            isSubmitCrashLogsEnabled: Boolean,
            accountSyncState: AccountSyncState,
            isSyncWithBitwardenFeatureEnabled: Boolean,
            sharedAccountsState: SharedVerificationCodesState,
        ): SettingsState {
            val currentYear = Year.now(clock)
            val copyrightInfo = "© Bitwarden Inc. 2015-$currentYear".asText()
            // Show sync with Bitwarden row if feature is enabled and the OS is supported:
            val shouldShowSyncWithBitwarden = isSyncWithBitwardenFeatureEnabled &&
                accountSyncState != AccountSyncState.OsVersionNotSupported
            // Show default save options only if the user had enabled sync with Bitwarden:
            // (They can enable it via the "Sync with Bitwarden" row.
            val shouldShowDefaultSaveOption = sharedAccountsState.isSyncWithBitwardenEnabled
            return SettingsState(
                appearance = SettingsState.Appearance(
                    language = appLanguage,
                    theme = appTheme,
                ),
                isUnlockWithBiometricsEnabled = unlockWithBiometricsEnabled,
                isSubmitCrashLogsEnabled = isSubmitCrashLogsEnabled,
                dialog = null,
                version = R.string.version
                    .asText()
                    .concat(": ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".asText()),
                copyrightInfo = copyrightInfo,
                defaultSaveOption = defaultSaveOption,
                showSyncWithBitwarden = shouldShowSyncWithBitwarden,
                showDefaultSaveOptionRow = shouldShowDefaultSaveOption,
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
    val defaultSaveOption: DefaultSaveOption,
    val isUnlockWithBiometricsEnabled: Boolean,
    val isSubmitCrashLogsEnabled: Boolean,
    val showSyncWithBitwarden: Boolean,
    val showDefaultSaveOptionRow: Boolean,
    val dialog: Dialog?,
    val version: Text,
    val copyrightInfo: Text,
) : Parcelable {

    /**
     * Models the dialog state for [SettingsViewModel].
     */
    @Parcelize
    sealed class Dialog : Parcelable {

        /**
         * Displays a loading dialog with a [message].
         */
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
     * Navigate to the Import screen.
     */
    data object NavigateToImport : SettingsEvent()

    /**
     * Navigate to the Backup web page.
     */
    data object NavigateToBackup : SettingsEvent()

    /**
     * Navigate to the Help Center web page.
     */
    data object NavigateToHelpCenter : SettingsEvent()

    /**
     * Navigate to the privacy policy web page.
     */
    data object NavigateToPrivacyPolicy : SettingsEvent()

    /**
     * Navigate to the Bitwarden account settings.
     */
    data object NavigateToBitwardenApp : SettingsEvent()

    /**
     * Navigate to the Bitwarden Play Store listing.
     */
    data object NavigateToBitwardenPlayStoreListing : SettingsEvent()
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
         * Display the loading screen with a [message].
         */
        data class Loading(
            val message: Text,
        ) : Dialog()
    }

    /**
     * Indicates the user clicked the Unlock with biometrics button.
     */
    sealed class SecurityClick : SettingsAction() {
        /**
         * Indicates the user clicked unlock with biometrics toggle.
         */
        data class UnlockWithBiometricToggle(val enabled: Boolean) : SecurityClick()
    }

    /**
     * Models actions for the Vault section of settings.
     */
    sealed class DataClick : SettingsAction() {

        /**
         * Indicates the user clicked export.
         */
        data object ExportClick : DataClick()

        /**
         * Indicates the user clicked import.
         */
        data object ImportClick : DataClick()

        /**
         * Indicates the user click backup.
         */
        data object BackupClick : DataClick()

        /**
         * Indicates the user clicked sync with Bitwarden.
         */
        data object SyncWithBitwardenClick : DataClick()

        /**
         * User confirmed a new [DeafultSaveOption].
         */
        data class DefaultSaveOptionUpdated(val option: DefaultSaveOption) : DataClick()
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

        /**
         * Indicates the user clicked submit crash logs toggle.
         */
        data class SubmitCrashLogsClick(val enabled: Boolean) : AboutClick()
    }

    /**
     * Models actions that the Settings screen itself may send.
     */
    sealed class Internal {

        /**
         * Indicates the biometrics key validation results has been received.
         */
        data class BiometricsKeyResultReceive(val result: BiometricsKeyResult) : SettingsAction()

        /**
         * Indicates that shared account state was updated.
         */
        data class SharedAccountsStateUpdated(
            val state: SharedVerificationCodesState,
        ) : SettingsAction()

        /**
         * Indicates that the default save option on disk was updated.
         */
        data class DefaultSaveOptionUpdated(
            val option: DefaultSaveOption,
        ) : SettingsAction()
    }
}
