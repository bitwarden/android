package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.data.manager.flightrecorder.FlightRecorderManager
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing and modifying settings state.
 */
interface SettingsRepository : FlightRecorderManager {

    /**
     * The [AppLanguage] for the current user.
     */
    var appLanguage: AppLanguage

    /**
     * The currently stored [AppTheme].
     */
    var appTheme: AppTheme

    /**
     * Tracks changes to the [AppTheme].
     */
    val appThemeStateFlow: StateFlow<AppTheme>

    /**
     * The currently stored expiration alert threshold.
     */
    var authenticatorAlertThresholdSeconds: Int

    /**
     * The currently stored [DefaultSaveOption].
     */
    var defaultSaveOption: DefaultSaveOption

    /**
     * The current setting for enabling dynamic colors.
     */
    var isDynamicColorsEnabled: Boolean

    /**
     * Tracks changes to the [isDynamicColorsEnabled] value.
     */
    val isDynamicColorsEnabledFlow: StateFlow<Boolean>

    /**
     * Flow that emits changes to [defaultSaveOption]
     */
    val defaultSaveOptionFlow: Flow<DefaultSaveOption>

    /**
     * Tracks changes to the expiration alert threshold.
     */
    val authenticatorAlertThresholdSecondsFlow: StateFlow<Int>

    /**
     * Whether the user has seen the Welcome tutorial.
     */
    var hasSeenWelcomeTutorial: Boolean

    /**
     * Tracks whether the user has seen the Welcome tutorial.
     */
    val hasSeenWelcomeTutorialFlow: StateFlow<Boolean>

    /**
     * Sets whether or not screen capture is allowed for the current user.
     */
    var isScreenCaptureAllowed: Boolean

    /**
     * Whether or not screen capture is allowed for the current user.
     */
    val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>

    /**
     * A set of Bitwarden account IDs that have previously been synced.
     */
    var previouslySyncedBitwardenAccountIds: Set<String>

    /**
     * The current setting for crash logging.
     */
    var isCrashLoggingEnabled: Boolean

    /**
     * Emits updates that track the [isCrashLoggingEnabled] value.
     */
    val isCrashLoggingEnabledFlow: Flow<Boolean>

    /**
     * Whether or not the user has previously dismissed the download Bitwarden action card.
     */
    var hasUserDismissedDownloadBitwardenCard: Boolean

    /**
     * Whether or not the user has previously dismissed the sync with Bitwarden action card.
     */
    var hasUserDismissedSyncWithBitwardenCard: Boolean
}
