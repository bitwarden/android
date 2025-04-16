package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.auth.repository.model.UserFingerprintResult
import com.x8bit.bitwarden.data.platform.manager.flightrecorder.FlightRecorderManager
import com.x8bit.bitwarden.data.platform.repository.model.BiometricsKeyResult
import com.x8bit.bitwarden.data.platform.repository.model.ClearClipboardFrequency
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.crypto.Cipher

/**
 * Provides an API for observing and modifying settings state.
 */
@Suppress("TooManyFunctions")
interface SettingsRepository : FlightRecorderManager {
    /**
     * The [AppLanguage] for the current user.
     */
    var appLanguage: AppLanguage

    /**
     * Tracks changes to the [AppLanguage].
     */
    val appLanguageStateFlow: StateFlow<AppLanguage>

    /**
     * The currently stored [AppTheme].
     */
    var appTheme: AppTheme

    /**
     * Tracks changes to the [AppTheme].
     */
    val appThemeStateFlow: StateFlow<AppTheme>

    /**
     * The current setting for enabling dynamic colors.
     */
    var isDynamicColorsEnabled: Boolean

    /**
     * Tracks changes to the [isDynamicColorsEnabled] value.
     */
    val isDynamicColorsEnabledFlow: StateFlow<Boolean>

    /**
     * Has the initial autofill dialog been shown to the user.
     */
    var initialAutofillDialogShown: Boolean

    /**
     * Whether the user has enabled syncing with the Bitwarden Authenticator app.
     */
    var isAuthenticatorSyncEnabled: Boolean

    /**
     * The currently stored last time the vault was synced.
     */
    var vaultLastSync: Instant?

    /**
     * Tracks changes to the [vaultLastSync].
     */
    val vaultLastSyncStateFlow: StateFlow<Instant?>

    /**
     * The current setting for getting login item icons.
     */
    var isIconLoadingDisabled: Boolean

    /**
     * Emits updates that track the [isIconLoadingDisabled] value.
     */
    val isIconLoadingDisabledFlow: Flow<Boolean>

    /**
     * The frequency in seconds at which we clear the clipboard.
     */
    var clearClipboardFrequency: ClearClipboardFrequency

    /**
     * The current setting for crash logging.
     */
    var isCrashLoggingEnabled: Boolean

    /**
     * Emits updates that track the [isCrashLoggingEnabled] value.
     */
    val isCrashLoggingEnabledFlow: Flow<Boolean>

    /**
     * The current status if a user has logged in or created an account.
     */
    var hasUserLoggedInOrCreatedAccount: Boolean

    /**
     * Emits updates that track the [hasUserLoggedInOrCreatedAccount] value.
     */
    val hasUserLoggedInOrCreatedAccountFlow: Flow<Boolean>

    /**
     * The [VaultTimeout] for the current user.
     */
    var vaultTimeout: VaultTimeout

    /**
     * The [VaultTimeoutAction] for the current user.
     */
    var vaultTimeoutAction: VaultTimeoutAction

    /**
     * The default [UriMatchType] for the current user that should be used when matching URIs for
     * items that have "default" as their chosen type.
     */
    var defaultUriMatchType: UriMatchType

    /**
     * Whether or not biometric unlocking is enabled for the current user.
     */
    val isUnlockWithBiometricsEnabled: Boolean

    /**
     * Emits updates whenever there is a change in the user status for biometric unlocking.
     */
    val isUnlockWithBiometricsEnabledFlow: Flow<Boolean>

    /**
     * Whether or not PIN unlocking is enabled for the current user.
     */
    val isUnlockWithPinEnabled: Boolean

    /**
     * Emits updates whenever there is a change in the user status for PIN unlocking.
     */
    val isUnlockWithPinEnabledFlow: Flow<Boolean>

    /**
     * Whether or not inline autofill is enabled for the current user.
     */
    var isInlineAutofillEnabled: Boolean

    /**
     * Whether or not the auto copying totp when autofilling is disabled for the current user.
     */
    var isAutoCopyTotpDisabled: Boolean

    /**
     * Whether or not the autofill save prompt is disabled for the current user.
     */
    var isAutofillSavePromptDisabled: Boolean

    /**
     * A list of blocked autofill URI's for the current user.
     */
    var blockedAutofillUris: List<String>

    /**
     * Emits updates whenever there is a change in the app's status for accessibility-based
     * autofill.
     *
     * Note that the correct value is only populated upon subscription so calling [StateFlow.value]
     * may result in an out-of-date value.
     */
    val isAccessibilityEnabledStateFlow: StateFlow<Boolean>

    /**
     * Emits updates whenever there is a change in the app's status for supporting autofill.
     *
     * Note that the correct value is only populated upon subscription so calling [StateFlow.value]
     * may result in an out-of-date value.
     */
    val isAutofillEnabledStateFlow: StateFlow<Boolean>

    /**
     * Sets whether or not screen capture is allowed for the current user.
     */
    var isScreenCaptureAllowed: Boolean

    /**
     * Whether or not screen capture is allowed for the current user.
     */
    val isScreenCaptureAllowedStateFlow: StateFlow<Boolean>

    /**
     * Disables autofill if it is currently enabled.
     */
    fun disableAutofill()

    /**
     * Gets the unique fingerprint phrase for the current user.
     */
    suspend fun getUserFingerprint(): UserFingerprintResult

    /**
     * Sets default values for various settings for the given [userId] if necessary. This is
     * typically used when logging into a new account.
     */
    fun setDefaultsIfNecessary(userId: String)

    /**
     * Gets updates for the [VaultTimeout] associated with the given [userId].
     */
    fun getVaultTimeoutStateFlow(userId: String): StateFlow<VaultTimeout>

    /**
     * Stores the given [vaultTimeout] for the given [userId].
     */
    fun storeVaultTimeout(userId: String, vaultTimeout: VaultTimeout)

    /**
     * Gets updates for the [VaultTimeoutAction] associated with the given [userId].
     *
     * Note that in cases where no value has been set, a default will be returned. Use
     * [isVaultTimeoutActionSet] to see if there is an actual value set.
     */
    fun getVaultTimeoutActionStateFlow(userId: String): StateFlow<VaultTimeoutAction>

    /**
     * Returns `true` if a [VaultTimeoutAction] is set for the given [userId], and `false`
     * otherwise.
     */
    fun isVaultTimeoutActionSet(userId: String): Boolean

    /**
     * Stores the given [VaultTimeoutAction] for the given [userId].
     */
    fun storeVaultTimeoutAction(userId: String, vaultTimeoutAction: VaultTimeoutAction?)

    /**
     * Gets updates for the pull to refresh enabled.
     */
    fun getPullToRefreshEnabledFlow(): StateFlow<Boolean>

    /**
     * Stores the given [isPullToRefreshEnabled] for the active user.
     */
    fun storePullToRefreshEnabled(isPullToRefreshEnabled: Boolean)

    /**
     * Stores the encrypted user key for biometrics, allowing it to be used to unlock the current
     * user's vault.
     */
    suspend fun setupBiometricsKey(cipher: Cipher): BiometricsKeyResult

    /**
     * Stores the given PIN, allowing it to be used to unlock the current user's vault.
     *
     * When [shouldRequireMasterPasswordOnRestart] is `true`, the user's master password is required
     * on app startup but they may use their PIN to unlock their vault if it becomes locked while
     * the app is still open.
     */
    fun storeUnlockPin(
        pin: String,
        shouldRequireMasterPasswordOnRestart: Boolean,
    )

    /**
     * Clears any previously set unlock PIN for the current user.
     */
    fun clearUnlockPin()

    /**
     * Returns true if the given [userId] has previously logged in on this device.
     *
     * This assumes the device storage has not been cleared since installation.
     */
    fun getUserHasLoggedInValue(userId: String): Boolean

    /**
     * Record that a user has logged in on this device.
     */
    fun storeUserHasLoggedInValue(userId: String)

    /**
     * Returns true if the given [userId] has previously registered for export via the credential
     * exchange protocol.
     */
    fun isVaultRegisteredForExport(userId: String): Boolean

    /**
     * Stores that the given [userId] has previously registered for export via the credential
     * exchange protocol.
     */
    fun storeVaultRegisteredForExport(userId: String, isRegistered: Boolean)

    /**
     * Gets updates for the [isVaultRegisteredForExport] value for the given [userId].
     */
    fun getVaultRegisteredForExportFlow(userId: String): StateFlow<Boolean>
}
