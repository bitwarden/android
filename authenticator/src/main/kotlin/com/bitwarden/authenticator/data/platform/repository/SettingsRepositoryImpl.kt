package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
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
    flightRecorderManager: FlightRecorderManager,
    dispatcherManager: DispatcherManager,
) : SettingsRepository,
    FlightRecorderManager by flightRecorderManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appLanguage: AppLanguage
        get() = settingsDiskSource.appLanguage ?: AppLanguage.DEFAULT
        set(value) {
            settingsDiskSource.appLanguage = value
        }

    override var appTheme: AppTheme by settingsDiskSource::appTheme

    override var authenticatorAlertThresholdSeconds = settingsDiskSource.getAlertThresholdSeconds()

    override var defaultSaveOption: DefaultSaveOption by settingsDiskSource::defaultSaveOption

    override val defaultSaveOptionFlow: Flow<DefaultSaveOption>
        by settingsDiskSource::defaultSaveOptionFlow

    override var isDynamicColorsEnabled: Boolean
        get() = settingsDiskSource.isDynamicColorsEnabled ?: false
        set(value) {
            settingsDiskSource.isDynamicColorsEnabled = value
        }

    override val isDynamicColorsEnabledFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .isDynamicColorsEnabledFlow
            .map { it ?: false }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = isDynamicColorsEnabled,
            )

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

    override var previouslySyncedBitwardenAccountIds: Set<String> by
    settingsDiskSource::previouslySyncedBitwardenAccountIds

    override val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>
        get() = settingsDiskSource.getScreenCaptureAllowedFlow()
            .map { isAllowed -> isAllowed ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = settingsDiskSource.getScreenCaptureAllowed()
                    ?: DEFAULT_IS_SCREEN_CAPTURE_ALLOWED,
            )

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
