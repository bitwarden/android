package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.manager.BiometricsEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val DEFAULT_IS_SCREEN_CAPTURE_ALLOWED = BuildConfig.DEBUG

/**
 * Primary implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val biometricsEncryptionManager: BiometricsEncryptionManager,
    private val authenticatorSdkSource: AuthenticatorSdkSource,
    dispatcherManager: DispatcherManager,
) : SettingsRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appLanguage: AppLanguage
        get() = settingsDiskSource.appLanguage ?: AppLanguage.DEFAULT
        set(value) {
            settingsDiskSource.appLanguage = value
        }

    override var appTheme: AppTheme by settingsDiskSource::appTheme

    override var authenticatorAlertThresholdSeconds = settingsDiskSource.getAlertThresholdSeconds()

    override val isUnlockWithBiometricsEnabled: Boolean
        get() = authDiskSource.getUserBiometricUnlockKey() != null

    override val appThemeStateFlow: StateFlow<AppTheme>
        get() = settingsDiskSource
            .appThemeFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.appTheme,
            )

    override val authenticatorAlertThresholdSecondsFlow: StateFlow<Int>
        get() = settingsDiskSource
            .getAlertThresholdSecondsFlow()
            .map { it }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.getAlertThresholdSeconds(),
            )

    override var hasSeenWelcomeTutorial: Boolean
        get() = settingsDiskSource.hasSeenWelcomeTutorial
        set(value) {
            settingsDiskSource.hasSeenWelcomeTutorial = value
        }

    override val hasSeenWelcomeTutorialFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .hasSeenWelcomeTutorialFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = hasSeenWelcomeTutorial,
            )

    override var isScreenCaptureAllowed: Boolean
        get() = settingsDiskSource.getScreenCaptureAllowed()
            ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED
        set(value) {
            settingsDiskSource.storeScreenCaptureAllowed(
                isScreenCaptureAllowed = value,
            )
        }

    override val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>
        get() = settingsDiskSource.getScreenCaptureAllowedFlow()
            .map { isAllowed -> isAllowed ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = settingsDiskSource.getScreenCaptureAllowed()
                    ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED,
            )

    override suspend fun setupBiometricsKey(): BiometricsKeyResult {
        biometricsEncryptionManager.setupBiometrics()
        return authenticatorSdkSource
            .generateBiometricsKey()
            .onSuccess {
                authDiskSource.storeUserBiometricUnlockKey(biometricsKey = it)
            }
            .fold(
                onSuccess = { BiometricsKeyResult.Success },
                onFailure = { BiometricsKeyResult.Error },
            )
    }

    override fun clearBiometricsKey() {
        authDiskSource.storeUserBiometricUnlockKey(biometricsKey = null)
    }

    override var isCrashLoggingEnabled: Boolean
        get() = settingsDiskSource.isCrashLoggingEnabled ?: true
        set(value) {
            settingsDiskSource.isCrashLoggingEnabled = value
        }

    override val isCrashLoggingEnabledFlow: Flow<Boolean>
        get() = settingsDiskSource
            .isCrashLoggingEnabledFlow
            .map { it ?: isCrashLoggingEnabled }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = isCrashLoggingEnabled,
            )

    override var hasUserDismissedDownloadBitwardenCard: Boolean
        get() = settingsDiskSource.hasUserDismissedDownloadBitwardenCard ?: false
        set(value) {
            settingsDiskSource.hasUserDismissedDownloadBitwardenCard = value
        }

    override var hasUserDismissedSyncWithBitwardenCard: Boolean
        get() = settingsDiskSource.hasUserDismissedSyncWithBitwardenCard ?: false
        set(value) {
            settingsDiskSource.hasUserDismissedSyncWithBitwardenCard = value
        }
}
