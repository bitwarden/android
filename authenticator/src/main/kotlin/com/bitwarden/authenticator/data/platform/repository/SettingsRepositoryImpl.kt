package com.bitwarden.authenticator.data.platform.repository

import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsKeyResult
import com.bitwarden.authenticator.data.platform.repository.model.BiometricsUnlockResult
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppLanguage
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.crypto.Cipher

private val DEFAULT_IS_SCREEN_CAPTURE_ALLOWED = BuildConfig.DEBUG

/**
 * Primary implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
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

    override val isUnlockWithBiometricsEnabled: Boolean
        get() = authDiskSource.getUserBiometricUnlockKey() != null

    override val isUnlockWithBiometricsEnabledFlow: StateFlow<Boolean>
        get() =
            authDiskSource
                .userBiometricUnlockKeyFlow
                .map { it != null }
                .stateIn(
                    scope = unconfinedScope,
                    started = SharingStarted.Eagerly,
                    initialValue = isUnlockWithBiometricsEnabled,
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

    override suspend fun setupBiometricsKey(cipher: Cipher): BiometricsKeyResult {
        return authenticatorSdkSource
            .generateBiometricsKey()
            .onSuccess { biometricsKey ->
                authDiskSource.storeUserBiometricUnlockKey(
                    biometricsKey = try {
                        cipher
                            .doFinal(biometricsKey.encodeToByteArray())
                            .toString(Charsets.ISO_8859_1)
                    } catch (e: GeneralSecurityException) {
                        Timber.w(e, "setupBiometricsKey failed encrypt the biometric key")
                        return BiometricsKeyResult.Error(error = e)
                    },
                )
                authDiskSource.userBiometricKeyInitVector = cipher.iv
            }
            .fold(
                onSuccess = { BiometricsKeyResult.Success },
                onFailure = { BiometricsKeyResult.Error(error = it) },
            )
    }

    override suspend fun unlockWithBiometrics(cipher: Cipher): BiometricsUnlockResult {
        val biometricsKey = authDiskSource
            .getUserBiometricUnlockKey()
            ?: return BiometricsUnlockResult.InvalidStateError(
                error = MissingPropertyException("Biometric key"),
            )
        val iv = authDiskSource.userBiometricKeyInitVector
        val decryptedUserKey = iv
            ?.let {
                try {
                    cipher
                        .doFinal(biometricsKey.toByteArray(Charsets.ISO_8859_1))
                        .decodeToString()
                } catch (e: GeneralSecurityException) {
                    Timber.w(e, "unlockWithBiometrics failed when decrypting biometrics key")
                    return BiometricsUnlockResult.BiometricDecodingError(error = e)
                }
            }
            ?: biometricsKey

        val encryptedBiometricsKey = if (iv == null) {
            // Attempting to setup an encrypted pin before unlocking, if this fails we send back
            // the biometrics error and users will need to sign in another way and re-setup
            // biometrics.
            try {
                cipher
                    .doFinal(decryptedUserKey.encodeToByteArray())
                    .toString(Charsets.ISO_8859_1)
            } catch (e: GeneralSecurityException) {
                Timber.w(e, "unlockWithBiometrics failed to migrate the user to IV encryption")
                return BiometricsUnlockResult.BiometricDecodingError(error = e)
            }
        } else {
            null
        }

        encryptedBiometricsKey?.let { key ->
            // If this key is present, we store it and the associated IV for future use
            // since we want to migrate the user to a more secure form of biometrics.
            authDiskSource.storeUserBiometricUnlockKey(biometricsKey = key)
            authDiskSource.userBiometricKeyInitVector = cipher.iv
        }

        return BiometricsUnlockResult.Success
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
