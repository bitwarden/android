package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

private const val APP_LANGUAGE_KEY = "appLocale"
private const val APP_THEME_KEY = "theme"
private const val PULL_TO_REFRESH_KEY = "syncOnRefresh"
private const val INLINE_AUTOFILL_ENABLED_KEY = "inlineAutofillEnabled"
private const val BLOCKED_AUTOFILL_URIS_KEY = "autofillBlacklistedUris"
private const val VAULT_LAST_SYNC_TIME = "vaultLastSyncTime"
private const val VAULT_TIMEOUT_ACTION_KEY = "vaultTimeoutAction"
private const val VAULT_TIME_IN_MINUTES_KEY = "vaultTimeout"
private const val DEFAULT_URI_MATCH_TYPE_KEY = "defaultUriMatch"
private const val DISABLE_AUTO_TOTP_COPY_KEY = "disableAutoTotpCopy"
private const val DISABLE_AUTOFILL_SAVE_PROMPT_KEY = "autofillDisableSavePrompt"
private const val DISABLE_ICON_LOADING_KEY = "disableFavicon"
private const val SCREEN_CAPTURE_ALLOW_KEY = "screenCaptureAllowed"
private const val SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY = "biometricIntegritySource"
private const val ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY = "accountBiometricIntegrityValid"
private const val CRASH_LOGGING_ENABLED_KEY = "crashLoggingEnabled"
private const val CLEAR_CLIPBOARD_INTERVAL_KEY = "clearClipboard"
private const val INITIAL_AUTOFILL_DIALOG_SHOWN = "addSitePromptShown"
private const val HAS_USER_LOGGED_IN_OR_CREATED_AN_ACCOUNT_KEY = "hasUserLoggedInOrCreatedAccount"

/**
 * Primary implementation of [SettingsDiskSource].
 */
@Suppress("TooManyFunctions")
class SettingsDiskSourceImpl(
    val sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    SettingsDiskSource {
    private val mutableAppThemeFlow = bufferedMutableSharedFlow<AppTheme>(replay = 1)

    private val mutableLastSyncFlowMap = mutableMapOf<String, MutableSharedFlow<Instant?>>()

    private val mutableVaultTimeoutActionFlowMap =
        mutableMapOf<String, MutableSharedFlow<VaultTimeoutAction?>>()

    private val mutableVaultTimeoutInMinutesFlowMap =
        mutableMapOf<String, MutableSharedFlow<Int?>>()

    private val mutablePullToRefreshEnabledFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

    private val mutableIsIconLoadingDisabledFlow = bufferedMutableSharedFlow<Boolean?>()

    private val mutableIsCrashLoggingEnabledFlow = bufferedMutableSharedFlow<Boolean?>()

    private val mutableHasUserLoggedInOrCreatedAccountFlow = bufferedMutableSharedFlow<Boolean?>()

    private val mutableScreenCaptureAllowedFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()

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

    override var initialAutofillDialogShown: Boolean?
        get() = getBoolean(key = INITIAL_AUTOFILL_DIALOG_SHOWN)
        set(value) {
            putBoolean(
                key = INITIAL_AUTOFILL_DIALOG_SHOWN,
                value = value,
            )
        }

    override var systemBiometricIntegritySource: String?
        get() = getString(key = SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY)
        set(value) {
            putString(key = SYSTEM_BIOMETRIC_INTEGRITY_SOURCE_KEY, value = value)
        }

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

    override var isCrashLoggingEnabled: Boolean?
        get() = getBoolean(key = CRASH_LOGGING_ENABLED_KEY)
        set(value) {
            putBoolean(key = CRASH_LOGGING_ENABLED_KEY, value = value)
            mutableIsCrashLoggingEnabledFlow.tryEmit(value)
        }

    override val isCrashLoggingEnabledFlow: Flow<Boolean?>
        get() = mutableIsCrashLoggingEnabledFlow
            .onSubscription { emit(getBoolean(CRASH_LOGGING_ENABLED_KEY)) }

    override var hasUserLoggedInOrCreatedAccount: Boolean?
        get() = getBoolean(key = HAS_USER_LOGGED_IN_OR_CREATED_AN_ACCOUNT_KEY)
        set(value) {
            putBoolean(key = HAS_USER_LOGGED_IN_OR_CREATED_AN_ACCOUNT_KEY, value = value)
            mutableHasUserLoggedInOrCreatedAccountFlow.tryEmit(value)
        }

    override val hasUserLoggedInOrCreatedAccountFlow: Flow<Boolean?>
        get() = mutableHasUserLoggedInOrCreatedAccountFlow
            .onSubscription { emit(getBoolean(HAS_USER_LOGGED_IN_OR_CREATED_AN_ACCOUNT_KEY)) }

    override fun clearData(userId: String) {
        storeVaultTimeoutInMinutes(userId = userId, vaultTimeoutInMinutes = null)
        storeVaultTimeoutAction(userId = userId, vaultTimeoutAction = null)
        storeDefaultUriMatchType(userId = userId, uriMatchType = null)
        storeAutoCopyTotpDisabled(userId = userId, isAutomaticallyCopyTotpDisabled = null)
        storeAutofillSavePromptDisabled(userId = userId, isAutofillSavePromptDisabled = null)
        storePullToRefreshEnabled(userId = userId, isPullToRefreshEnabled = null)
        storeInlineAutofillEnabled(userId = userId, isInlineAutofillEnabled = null)
        storeBlockedAutofillUris(userId = userId, blockedAutofillUris = null)
        storeLastSyncTime(userId = userId, lastSyncTime = null)
        storeClearClipboardFrequencySeconds(userId = userId, frequency = null)
        removeWithPrefix(prefix = ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY.appendIdentifier(userId))

        // The following are intentionally not cleared so they can be
        // restored after logging out and back in:
        // - screen capture allowed
    }

    override fun getAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
    ): Boolean? =
        getBoolean(
            key = ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY
                .appendIdentifier(userId)
                .appendIdentifier(systemBioIntegrityState),
        )

    override fun storeAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
        value: Boolean?,
    ) {
        putBoolean(
            key = ACCOUNT_BIOMETRIC_INTEGRITY_VALID_KEY
                .appendIdentifier(userId)
                .appendIdentifier(systemBioIntegrityState),
            value = value,
        )
    }

    override fun getAutoCopyTotpDisabled(userId: String): Boolean? =
        getBoolean(key = DISABLE_AUTO_TOTP_COPY_KEY.appendIdentifier(userId))

    override fun storeAutoCopyTotpDisabled(
        userId: String,
        isAutomaticallyCopyTotpDisabled: Boolean?,
    ) {
        putBoolean(
            key = DISABLE_AUTO_TOTP_COPY_KEY.appendIdentifier(userId),
            value = isAutomaticallyCopyTotpDisabled,
        )
    }

    override fun getLastSyncTime(userId: String): Instant? =
        getLong(key = VAULT_LAST_SYNC_TIME.appendIdentifier(userId))
            ?.let { Instant.ofEpochMilli(it) }

    override fun storeLastSyncTime(userId: String, lastSyncTime: Instant?) {
        putLong(
            key = VAULT_LAST_SYNC_TIME.appendIdentifier(userId),
            value = lastSyncTime?.toEpochMilli(),
        )
        getMutableLastSyncFlow(userId = userId).tryEmit(lastSyncTime)
    }

    override fun getLastSyncTimeFlow(userId: String): Flow<Instant?> =
        getMutableLastSyncFlow(userId = userId)
            .onSubscription { emit(getLastSyncTime(userId = userId)) }

    override fun getVaultTimeoutInMinutes(userId: String): Int? =
        getInt(key = VAULT_TIME_IN_MINUTES_KEY.appendIdentifier(userId))

    override fun getVaultTimeoutInMinutesFlow(userId: String): Flow<Int?> =
        getMutableVaultTimeoutInMinutesFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutInMinutes(userId = userId)) }

    override fun storeVaultTimeoutInMinutes(
        userId: String,
        vaultTimeoutInMinutes: Int?,
    ) {
        putInt(
            key = VAULT_TIME_IN_MINUTES_KEY.appendIdentifier(userId),
            value = vaultTimeoutInMinutes,
        )
        getMutableVaultTimeoutInMinutesFlow(userId = userId).tryEmit(vaultTimeoutInMinutes)
    }

    override fun getClearClipboardFrequencySeconds(userId: String): Int? =
        getInt(key = CLEAR_CLIPBOARD_INTERVAL_KEY.appendIdentifier(userId))

    override fun storeClearClipboardFrequencySeconds(userId: String, frequency: Int?) {
        putInt(
            key = CLEAR_CLIPBOARD_INTERVAL_KEY.appendIdentifier(userId),
            value = frequency,
        )
    }

    override fun getVaultTimeoutAction(userId: String): VaultTimeoutAction? =
        getString(key = VAULT_TIMEOUT_ACTION_KEY.appendIdentifier(userId))?.let { storedValue ->
            VaultTimeoutAction.entries.firstOrNull { storedValue == it.value }
        }

    override fun getVaultTimeoutActionFlow(userId: String): Flow<VaultTimeoutAction?> =
        getMutableVaultTimeoutActionFlow(userId = userId)
            .onSubscription { emit(getVaultTimeoutAction(userId = userId)) }

    override fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    ) {
        putString(
            key = VAULT_TIMEOUT_ACTION_KEY.appendIdentifier(userId),
            value = vaultTimeoutAction?.value,
        )
        getMutableVaultTimeoutActionFlow(userId = userId).tryEmit(vaultTimeoutAction)
    }

    override fun getDefaultUriMatchType(userId: String): UriMatchType? =
        getInt(key = DEFAULT_URI_MATCH_TYPE_KEY.appendIdentifier(userId))?.let { storedValue ->
            UriMatchType.entries.find { it.value == storedValue }
        }

    override fun storeDefaultUriMatchType(
        userId: String,
        uriMatchType: UriMatchType?,
    ) {
        putInt(
            key = DEFAULT_URI_MATCH_TYPE_KEY.appendIdentifier(userId),
            value = uriMatchType?.value,
        )
    }

    override fun getAutofillSavePromptDisabled(userId: String): Boolean? =
        getBoolean(key = DISABLE_AUTOFILL_SAVE_PROMPT_KEY.appendIdentifier(userId))

    override fun storeAutofillSavePromptDisabled(
        userId: String,
        isAutofillSavePromptDisabled: Boolean?,
    ) {
        putBoolean(
            key = DISABLE_AUTOFILL_SAVE_PROMPT_KEY.appendIdentifier(userId),
            value = isAutofillSavePromptDisabled,
        )
    }

    override fun getPullToRefreshEnabled(userId: String): Boolean? =
        getBoolean(key = PULL_TO_REFRESH_KEY.appendIdentifier(userId))

    override fun getPullToRefreshEnabledFlow(userId: String): Flow<Boolean?> =
        getMutablePullToRefreshEnabledFlowMap(userId = userId)
            .onSubscription { emit(getPullToRefreshEnabled(userId = userId)) }

    override fun storePullToRefreshEnabled(userId: String, isPullToRefreshEnabled: Boolean?) {
        putBoolean(
            key = PULL_TO_REFRESH_KEY.appendIdentifier(userId),
            value = isPullToRefreshEnabled,
        )
        getMutablePullToRefreshEnabledFlowMap(userId = userId).tryEmit(isPullToRefreshEnabled)
    }

    override fun getInlineAutofillEnabled(userId: String): Boolean? =
        getBoolean(key = INLINE_AUTOFILL_ENABLED_KEY.appendIdentifier(userId))

    override fun storeInlineAutofillEnabled(
        userId: String,
        isInlineAutofillEnabled: Boolean?,
    ) {
        putBoolean(
            key = INLINE_AUTOFILL_ENABLED_KEY.appendIdentifier(userId),
            value = isInlineAutofillEnabled,
        )
    }

    override fun getBlockedAutofillUris(userId: String): List<String>? =
        getString(key = BLOCKED_AUTOFILL_URIS_KEY.appendIdentifier(userId))?.let {
            json.decodeFromStringOrNull(it)
        }

    override fun storeBlockedAutofillUris(
        userId: String,
        blockedAutofillUris: List<String>?,
    ) {
        putString(
            key = BLOCKED_AUTOFILL_URIS_KEY.appendIdentifier(userId),
            value = blockedAutofillUris?.let { json.encodeToString(it) },
        )
    }

    private fun getMutableLastSyncFlow(
        userId: String,
    ): MutableSharedFlow<Instant?> =
        mutableLastSyncFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableVaultTimeoutActionFlow(
        userId: String,
    ): MutableSharedFlow<VaultTimeoutAction?> =
        mutableVaultTimeoutActionFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableVaultTimeoutInMinutesFlow(
        userId: String,
    ): MutableSharedFlow<Int?> =
        mutableVaultTimeoutInMinutesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutablePullToRefreshEnabledFlowMap(
        userId: String,
    ): MutableSharedFlow<Boolean?> =
        mutablePullToRefreshEnabledFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableScreenCaptureAllowedFlow(userId: String): MutableSharedFlow<Boolean?> =
        mutableScreenCaptureAllowedFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    override fun getScreenCaptureAllowed(userId: String): Boolean? {
        return getBoolean(key = SCREEN_CAPTURE_ALLOW_KEY.appendIdentifier(userId))
    }

    override fun getScreenCaptureAllowedFlow(userId: String): Flow<Boolean?> =
        getMutableScreenCaptureAllowedFlow(userId)
            .onSubscription { emit(getScreenCaptureAllowed(userId)) }

    override fun storeScreenCaptureAllowed(
        userId: String,
        isScreenCaptureAllowed: Boolean?,
    ) {
        putBoolean(
            key = SCREEN_CAPTURE_ALLOW_KEY.appendIdentifier(userId),
            value = isScreenCaptureAllowed,
        )
        getMutableScreenCaptureAllowedFlow(userId).tryEmit(isScreenCaptureAllowed)
    }
}
