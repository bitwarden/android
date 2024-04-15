package com.x8bit.bitwarden.authenticator.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.authenticator.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.x8bit.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription

private const val APP_THEME_KEY = "$BASE_KEY:theme"
private const val APP_LANGUAGE_KEY = "$BASE_KEY:appLocale"
private const val SCREEN_CAPTURE_ALLOW_KEY = "$BASE_KEY:screenCaptureAllowed"
private const val ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY = "$BASE_KEY:accountBiometricIntegrityValid"
private const val ALERT_THRESHOLD_SECONDS_KEY = "$BASE_KEY:alertThresholdSeconds"
private const val DISABLE_ICON_LOADING_KEY = "$BASE_KEY:disableFavicon"
private const val FIRST_LAUNCH_KEY = "$BASE_KEY:hasSeenWelcomeTutorial"

/**
 * Primary implementation of [SettingsDiskSource].
 */
class SettingsDiskSourceImpl(
    sharedPreferences: SharedPreferences,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    SettingsDiskSource {
    private val mutableAppThemeFlow =
        bufferedMutableSharedFlow<AppTheme>(replay = 1)

    private val mutableScreenCaptureAllowedFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableIsIconLoadingDisabledFlow =
        bufferedMutableSharedFlow<Boolean?>()

    private val mutableAlertThresholdSecondsFlow =
        bufferedMutableSharedFlow<Int>()

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

    override var isIconLoadingDisabled: Boolean?
        get() = getBoolean(key = DISABLE_ICON_LOADING_KEY)
        set(value) {
            putBoolean(key = DISABLE_ICON_LOADING_KEY, value = value)
            mutableIsIconLoadingDisabledFlow.tryEmit(value)
        }

    override val isIconLoadingDisabledFlow: Flow<Boolean?>
        get() = mutableIsIconLoadingDisabledFlow
            .onSubscription { emit(getBoolean(DISABLE_ICON_LOADING_KEY)) }

    override var hasSeenWelcomeTutorial: Boolean
        get() = getBoolean(key = FIRST_LAUNCH_KEY) ?: false
        set(value) {
            putBoolean(key = FIRST_LAUNCH_KEY, value)
            mutableFirstLaunchFlow.tryEmit(hasSeenWelcomeTutorial)
        }

    override val hasSeenWelcomeTutorialFlow: Flow<Boolean>
        get() = mutableFirstLaunchFlow.onSubscription { emit(hasSeenWelcomeTutorial) }

    override fun storeAlertThresholdSeconds(thresholdSeconds: Int) {
        putInt(
            ALERT_THRESHOLD_SECONDS_KEY,
            thresholdSeconds
        )
        mutableAlertThresholdSecondsFlow.tryEmit(thresholdSeconds)
    }

    override fun getAlertThresholdSeconds(): Int {
        return getInt(ALERT_THRESHOLD_SECONDS_KEY, default = 7) ?: 7
    }

    override fun getAlertThresholdSecondsFlow(): Flow<Int> = mutableAlertThresholdSecondsFlow
        .onSubscription { emit(getAlertThresholdSeconds()) }

    override fun clearData(userId: String) {
        storeScreenCaptureAllowed(userId = userId, isScreenCaptureAllowed = null)
        removeWithPrefix(prefix = "${ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY}_$userId")
    }

    private fun getMutableScreenCaptureAllowedFlow(userId: String): MutableSharedFlow<Boolean?> =
        mutableScreenCaptureAllowedFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    override fun getScreenCaptureAllowed(userId: String): Boolean? {
        return getBoolean(key = "${SCREEN_CAPTURE_ALLOW_KEY}_$userId")
    }

    override fun getScreenCaptureAllowedFlow(userId: String): Flow<Boolean?> =
        getMutableScreenCaptureAllowedFlow(userId)
            .onSubscription { emit(getScreenCaptureAllowed(userId)) }

    override fun storeScreenCaptureAllowed(
        userId: String,
        isScreenCaptureAllowed: Boolean?,
    ) {
        putBoolean(
            key = "${SCREEN_CAPTURE_ALLOW_KEY}_$userId",
            value = isScreenCaptureAllowed,
        )
        getMutableScreenCaptureAllowedFlow(userId).tryEmit(isScreenCaptureAllowed)
    }
}
