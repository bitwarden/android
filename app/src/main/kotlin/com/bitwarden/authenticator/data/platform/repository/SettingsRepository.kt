package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API for observing and modifying settings state.
 */
interface SettingsRepository {

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
     * Flow that emits changes to [defaultSaveOption]
     */
    val defaultSaveOptionFlow: Flow<DefaultSaveOption>

    /**
     * Whether or not biometric unlocking is enabled for the current user.
     */
    val isUnlockWithBiometricsEnabled: Boolean

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
     * A set of Bitwarden account IDs that have previously been synced.
     */
    var previouslySyncedBitwardenAccountIds: Set<String>

    /**
     * Whether or not screen capture is allowed for the current user.
     */
    val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>

    /**
     * Clears any previously stored encrypted user key used with biometrics for the current user.
     */
    fun clearBiometricsKey()

    /**
     * Stores the encrypted user key for biometrics, allowing it to be used to unlock the current
     * user's vault.
     */
    suspend fun setupBiometricsKey(): BiometricsKeyResult

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
