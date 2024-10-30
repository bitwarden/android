package com.bitwarden.authenticator.data.platform.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

private const val APP_THEME_KEY = "$BASE_KEY:theme"
private const val APP_LANGUAGE_KEY = "$BASE_KEY:appLocale"
private const val DEFAULT_SAVE_OPTION_KEY = "$BASE_KEY:defaultSaveOption"
private const val SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY = "$BASE_KEY:biometricIntegritySource"
private const val ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY = "$BASE_KEY:accountBiometricIntegrityValid"
private const val ALERT_THRESHOLD_SECONDS_KEY = "$BASE_KEY:alertThresholdSeconds"
private const val FIRST_LAUNCH_KEY = "$BASE_KEY:hasSeenWelcomeTutorial"
private const val CRASH_LOGGING_ENABLED_KEY = "$BASE_KEY:crashLoggingEnabled"
private const val SCREEN_CAPTURE_ALLOW_KEY = "screenCaptureAllowed"
private const val HAS_USER_DISMISSED_DOWNLOAD_BITWARDEN_KEY =
    "$BASE_KEY:hasUserDismissedDownloadBitwardenCard"
private const val HAS_USER_DISMISSED_SYNC_WITH_BITWARDEN_KEY =
    "$BASE_KEY:hasUserDismissedSyncWithBitwardenCard"
private const val PREVIOUSLY_SYNCED_BITWARDEN_ACCOUNT_IDS_KEY =
    "$BASE_KEY:previouslySyncedBitwardenAccountIds"
private const val DEFAULT_ALERT_THRESHOLD_SECONDS = 7

/**
 * Primary implementation of [SettingsDiskSource].
 */
class SettingsDiskSourceImpl(
    sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    SettingsDiskSource {
    private val mutableAppThemeFlow =
        bufferedMutableSharedFlow<AppTheme>(replay = 1)

    private val mutableScreenCaptureAllowedFlow =
        bufferedMutableSharedFlow<Boolean?>()

    private val mutableAlertThresholdSecondsFlow =
        bufferedMutableSharedFlow<Int>()

    private val mutableIsCrashLoggingEnabledFlow =
        bufferedMutableSharedFlow<Boolean?>()

    override var appLanguage: AppLanguage?
        get() = getString(key = APP_LANGUAGE_KEY)
            ?.let { storedValue ->
                AppLanguage.entries.firstOrNull { storedValue == it.localeName }
            }
        set(value) {
            putString(
                key = APP_LANGUAGE_KEY,
                value = value?.localeName,
            )
        }

    private val mutableFirstLaunchFlow =
        bufferedMutableSharedFlow<Boolean>()

    override var appTheme: AppTheme
        get() = getString(key = APP_THEME_KEY)
            ?.let { storedValue ->
                AppTheme.entries.firstOrNull { storedValue == it.value }
            }
            ?: AppTheme.DEFAULT
        set(newValue) {
            putString(
                key = APP_THEME_KEY,
                value = newValue.value,
            )
            mutableAppThemeFlow.tryEmit(appTheme)
        }

    override val appThemeFlow: Flow<AppTheme>
        get() = mutableAppThemeFlow
            .onSubscription { emit(appTheme) }

    override var defaultSaveOption: DefaultSaveOption
        get() = getString(key = DEFAULT_SAVE_OPTION_KEY)
            ?.let { storedValue ->
                DefaultSaveOption.entries.firstOrNull { storedValue == it.value }
            }
            ?: DefaultSaveOption.NONE
        set(newValue) {
            putString(
                key = DEFAULT_SAVE_OPTION_KEY,
                value = newValue.value,
            )
        }

    override var systemBiometricIntegritySource: String?
        get() = getString(key = SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY)
        set(value) {
            putString(key = SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY, value = value)
        }

    override var hasSeenWelcomeTutorial: Boolean
        get() = getBoolean(key = FIRST_LAUNCH_KEY) ?: false
        set(value) {
            putBoolean(key = FIRST_LAUNCH_KEY, value)
            mutableFirstLaunchFlow.tryEmit(hasSeenWelcomeTutorial)
        }

    override var previouslySyncedBitwardenAccountIds: Set<String>
        get() = getStringSet(
            key = PREVIOUSLY_SYNCED_BITWARDEN_ACCOUNT_IDS_KEY,
            default = emptySet(),
        ) ?: emptySet()
        set(value) {
            putStringSet(
                key = PREVIOUSLY_SYNCED_BITWARDEN_ACCOUNT_IDS_KEY,
                value = value,
            )
        }

    override val hasSeenWelcomeTutorialFlow: Flow<Boolean>
        get() = mutableFirstLaunchFlow.onSubscription { emit(hasSeenWelcomeTutorial) }

    override var isCrashLoggingEnabled: Boolean?
        get() = getBoolean(key = CRASH_LOGGING_ENABLED_KEY)
        set(value) {
            putBoolean(key = CRASH_LOGGING_ENABLED_KEY, value = value)
            mutableIsCrashLoggingEnabledFlow.tryEmit(value)
        }

    override val isCrashLoggingEnabledFlow: Flow<Boolean?>
        get() = mutableIsCrashLoggingEnabledFlow
            .onSubscription { emit(getBoolean(CRASH_LOGGING_ENABLED_KEY)) }

    override var hasUserDismissedDownloadBitwardenCard: Boolean?
        get() = getBoolean(HAS_USER_DISMISSED_DOWNLOAD_BITWARDEN_KEY, null)
        set(value) {
            putBoolean(HAS_USER_DISMISSED_DOWNLOAD_BITWARDEN_KEY, value)
        }

    override var hasUserDismissedSyncWithBitwardenCard: Boolean?
        get() = getBoolean(HAS_USER_DISMISSED_SYNC_WITH_BITWARDEN_KEY, null)
        set(value) {
            putBoolean(HAS_USER_DISMISSED_SYNC_WITH_BITWARDEN_KEY, value)
        }

    override fun storeAlertThresholdSeconds(thresholdSeconds: Int) {
        putInt(
            ALERT_THRESHOLD_SECONDS_KEY,
            thresholdSeconds,
        )
        mutableAlertThresholdSecondsFlow.tryEmit(thresholdSeconds)
    }

    override fun getAlertThresholdSeconds() =
        getInt(ALERT_THRESHOLD_SECONDS_KEY, default = DEFAULT_ALERT_THRESHOLD_SECONDS)
            ?: DEFAULT_ALERT_THRESHOLD_SECONDS

    override fun getAlertThresholdSecondsFlow(): Flow<Int> = mutableAlertThresholdSecondsFlow
        .onSubscription { emit(getAlertThresholdSeconds()) }

    override fun getAccountBiometricIntegrityValidity(
        systemBioIntegrityState: String,
    ): Boolean? =
        getBoolean(
            key = "${ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY}_$systemBioIntegrityState",
        )

    override fun storeAccountBiometricIntegrityValidity(
        systemBioIntegrityState: String,
        value: Boolean?,
    ) {
        putBoolean(
            key = "${ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY}_$systemBioIntegrityState",
            value = value,
        )
    }

    override fun getScreenCaptureAllowed(): Boolean? {
        return getBoolean(key = SCREEN_CAPTURE_ALLOW_KEY)
    }

    override fun getScreenCaptureAllowedFlow(): Flow<Boolean?> = mutableScreenCaptureAllowedFlow
        .onSubscription { emit(getScreenCaptureAllowed()) }

    override fun storeScreenCaptureAllowed(
        isScreenCaptureAllowed: Boolean?,
    ) {
        putBoolean(
            key = SCREEN_CAPTURE_ALLOW_KEY,
            value = isScreenCaptureAllowed,
        )
        mutableScreenCaptureAllowedFlow.tryEmit(isScreenCaptureAllowed)
    }
}
