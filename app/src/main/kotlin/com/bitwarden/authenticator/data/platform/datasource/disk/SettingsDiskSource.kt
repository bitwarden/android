package com.bitwarden.authenticator.data.platform.datasource.disk

import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for general settings-related disk information.
 */
interface SettingsDiskSource {

    /**
     * The currently persisted app language (or `null` if not set).
     */
    var appLanguage: AppLanguage?

    /**
     * The currently persisted app theme (or `null` if not set).
     */
    var appTheme: AppTheme

    /**
     * Emits updates that track [appTheme].
     */
    val appThemeFlow: Flow<AppTheme>

    /**
     * The currently persisted biometric integrity source for the system.
     */
    var systemBiometricIntegritySource: String?

    /**
     * Tracks whether user has seen the Welcome tutorial.
     */
    var hasSeenWelcomeTutorial: Boolean

    /**
     * Emits update that track [hasSeenWelcomeTutorial]
     */
    val hasSeenWelcomeTutorialFlow: Flow<Boolean>

    /**
     * The current setting for if crash logging is enabled.
     */
    var isCrashLoggingEnabled: Boolean?

    /**
     * The current setting for if crash logging is enabled.
     */
    val isCrashLoggingEnabledFlow: Flow<Boolean?>

    /**
     * Stores the threshold at which users are alerted that an items validity period is nearing
     * expiration.
     */
    fun storeAlertThresholdSeconds(thresholdSeconds: Int)

    /**
     * Gets the threshold at which users are alerted that an items validity period is nearing
     * expiration.
     */
    fun getAlertThresholdSeconds(): Int

    /**
     * Emits updates that track the threshold at which users are alerted that an items validity
     * period is nearing expiration.
     */
    fun getAlertThresholdSecondsFlow(): Flow<Int>

    /**
     * Retrieves the biometric integrity validity for the given [systemBioIntegrityState].
     */
    fun getAccountBiometricIntegrityValidity(
        systemBioIntegrityState: String,
    ): Boolean?

    /**
     * Stores the biometric integrity validity for the given [systemBioIntegrityState].
     */
    fun storeAccountBiometricIntegrityValidity(
        systemBioIntegrityState: String,
        value: Boolean?,
    )

    /**
     * Gets whether or not the user has enabled screen capture.
     */
    fun getScreenCaptureAllowed(): Boolean?

    /**
     * Emits updates that track [getScreenCaptureAllowed].
     */
    fun getScreenCaptureAllowedFlow(): Flow<Boolean?>

    /**
     * Stores whether or not [isScreenCaptureAllowed].
     */
    fun storeScreenCaptureAllowed(isScreenCaptureAllowed: Boolean?)
}
