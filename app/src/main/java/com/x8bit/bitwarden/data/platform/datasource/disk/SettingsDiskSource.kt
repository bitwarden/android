package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Primary access point for general settings-related disk information.
 */
@Suppress("TooManyFunctions")
interface SettingsDiskSource {

    /**
     * The currently persisted app language (or `null` if not set).
     */
    var appLanguage: AppLanguage?

    /**
     * Has the initial autofill dialog been shown to the user.
     */
    var initialAutofillDialogShown: Boolean?

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
     * The currently persisted setting for getting login item icons (or `null` if not set).
     */
    var isIconLoadingDisabled: Boolean?

    /**
     * Emits updates that track [isIconLoadingDisabled].
     */
    val isIconLoadingDisabledFlow: Flow<Boolean?>

    /**
     * The current setting for if crash logging is enabled.
     */
    var isCrashLoggingEnabled: Boolean?

    /**
     * The current setting for if crash logging is enabled.
     */
    val isCrashLoggingEnabledFlow: Flow<Boolean?>

    /**
     * The current status if a user has logged in or created an account.
     */
    var hasUserLoggedInOrCreatedAccount: Boolean?

    /**
     * Emits updates that track [hasUserLoggedInOrCreatedAccount].
     */
    val hasUserLoggedInOrCreatedAccountFlow: Flow<Boolean?>

    /**
     * Clears all the settings data for the given user.
     */
    fun clearData(userId: String)

    /**
     * Retrieves the biometric integrity validity for the given [userId] and
     * [systemBioIntegrityState].
     */
    fun getAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
    ): Boolean?

    /**
     * Stores the biometric integrity validity for the given [userId] and [systemBioIntegrityState].
     */
    fun storeAccountBiometricIntegrityValidity(
        userId: String,
        systemBioIntegrityState: String,
        value: Boolean?,
    )

    /**
     * Retrieves the preference indicating whether the TOTP code should be automatically copied to
     * the clipboard for autofill suggestions associated with the specified [userId].
     */
    fun getAutoCopyTotpDisabled(userId: String): Boolean?

    /**
     * Stores the given [isAutomaticallyCopyTotpDisabled] for the given [userId].
     */
    fun storeAutoCopyTotpDisabled(
        userId: String,
        isAutomaticallyCopyTotpDisabled: Boolean?,
    )

    /**
     * Gets the last time the app synced the vault data for a given [userId] (or `null` if the
     * vault has never been synced).
     */
    fun getLastSyncTime(userId: String): Instant?

    /**
     * Emits updates that track [getLastSyncTime] for the given [userId]. This will replay the
     * last known value, if any.
     */
    fun getLastSyncTimeFlow(userId: String): Flow<Instant?>

    /**
     * Stores the given [lastSyncTime] for the given [userId].
     */
    fun storeLastSyncTime(userId: String, lastSyncTime: Instant?)

    /**
     * Gets the current vault timeout (in minutes) for the given [userId] (or `null` if the vault
     * should never time out).
     */
    fun getVaultTimeoutInMinutes(userId: String): Int?

    /**
     * Emits updates that track [getVaultTimeoutInMinutes] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getVaultTimeoutInMinutesFlow(userId: String): Flow<Int?>

    /**
     * Stores the given [vaultTimeoutInMinutes] for the given [userId].
     */
    fun storeVaultTimeoutInMinutes(userId: String, vaultTimeoutInMinutes: Int?)

    /**
     * Gets the current [VaultTimeoutAction] for the given [userId].
     */
    fun getVaultTimeoutAction(userId: String): VaultTimeoutAction?

    /**
     * Emits updates that track [getVaultTimeoutAction] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getVaultTimeoutActionFlow(userId: String): Flow<VaultTimeoutAction?>

    /**
     * Stores the given [vaultTimeoutAction] for the given [userId].
     */
    fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    )

    /**
     * Gets the clipboard clearing frequency in seconds for the given [userId].
     */
    fun getClearClipboardFrequencySeconds(userId: String): Int?

    /**
     * Stores the clipboard clearing frequency in seconds for the given [userId].
     */
    fun storeClearClipboardFrequencySeconds(userId: String, frequency: Int?)

    /**
     * Gets the default [UriMatchType] for the given [userId].
     */
    fun getDefaultUriMatchType(userId: String): UriMatchType?

    /**
     * Stores the given default [uriMatchType] for the given [userId].
     */
    fun storeDefaultUriMatchType(userId: String, uriMatchType: UriMatchType?)

    /**
     * Gets the value for whether or not the autofill save prompt should be disabled for the
     * given [userId].
     */
    fun getAutofillSavePromptDisabled(userId: String): Boolean?

    /**
     * Stores the given [isAutofillSavePromptDisabled] for the given [userId].
     */
    fun storeAutofillSavePromptDisabled(
        userId: String,
        isAutofillSavePromptDisabled: Boolean?,
    )

    /**
     * Gets the current state of the pull to refresh feature for the given [userId].
     */
    fun getPullToRefreshEnabled(userId: String): Boolean?

    /**
     * Emits updates that track [getPullToRefreshEnabled] for the given [userId]. This will replay
     * the last known value, if any.
     */
    fun getPullToRefreshEnabledFlow(userId: String): Flow<Boolean?>

    /**
     * Stores the given [isPullToRefreshEnabled] for the given [userId].
     */
    fun storePullToRefreshEnabled(userId: String, isPullToRefreshEnabled: Boolean?)

    /**
     * Gets the value determining if inline autofill is enabled for the given [userId].
     */
    fun getInlineAutofillEnabled(userId: String): Boolean?

    /**
     * Stores the given [isInlineAutofillEnabled] value for the given [userId].
     */
    fun storeInlineAutofillEnabled(userId: String, isInlineAutofillEnabled: Boolean?)

    /**
     * Gets a list of blocked autofill URI's for the given [userId].
     */
    fun getBlockedAutofillUris(userId: String): List<String>?

    /**
     * Stores the list of [blockedAutofillUris] for the given [userId].
     */
    fun storeBlockedAutofillUris(
        userId: String,
        blockedAutofillUris: List<String>?,
    )

    /**
     * Gets whether or not the given [userId] has enabled screen capture.
     */
    fun getScreenCaptureAllowed(userId: String): Boolean?

    /**
     * Emits updates that track [getScreenCaptureAllowed] for the given [userId].
     */
    fun getScreenCaptureAllowedFlow(userId: String): Flow<Boolean?>

    /**
     * Stores whether or not [isScreenCaptureAllowed] for the given [userId].
     */
    fun storeScreenCaptureAllowed(userId: String, isScreenCaptureAllowed: Boolean?)

    /**
     * Records a user sign in for the given [userId]. This data is expected to remain on
     * disk until storage is cleared or the app is uninstalled.
     */
    fun storeUseHasLoggedInPreviously(userId: String)

    /**
     * Checks if a user has signed in previously for the given [userId].
     *
     * @see [storeUseHasLoggedInPreviously]
     */
    fun getUserHasSignedInPreviously(userId: String): Boolean

    /**
     * Gets whether or not the given [userId] has signalled they want to enable autofill in
     * onboarding.
     */
    fun getShowAutoFillSettingBadge(userId: String): Boolean?

    /**
     * Stores the given value for whether or not the given [userId] has signalled they want to
     * enable autofill in onboarding.
     */
    fun storeShowAutoFillSettingBadge(userId: String, showBadge: Boolean?)

    /**
     * Emits updates that track [getShowAutoFillSettingBadge] for the given [userId].
     */
    fun getShowAutoFillSettingBadgeFlow(userId: String): Flow<Boolean?>

    /**
     * Gets whether or not the given [userId] has signalled they want to enable unlock options
     * later, during onboarding.
     */
    fun getShowUnlockSettingBadge(userId: String): Boolean?

    /**
     * Stores the given value for whether or not the given [userId] has signalled they want to
     * set up unlock options later, during onboarding.
     */
    fun storeShowUnlockSettingBadge(userId: String, showBadge: Boolean?)

    /**
     * Emits updates that track [getShowUnlockSettingBadge] for the given [userId].
     */
    fun getShowUnlockSettingBadgeFlow(userId: String): Flow<Boolean?>

    /**
     * Gets whether or not the given [userId] has signalled they want to import logins later.
     */
    fun getShowImportLoginsSettingBadge(userId: String): Boolean?

    /**
     * Stores the given value for whether or not the given [userId] has signalled they want to
     * set import logins later, during first time usage.
     */
    fun storeShowImportLoginsSettingBadge(userId: String, showBadge: Boolean?)

    /**
     * Emits updates that track [getShowImportLoginsSettingBadge] for the given [userId].
     */
    fun getShowImportLoginsSettingBadgeFlow(userId: String): Flow<Boolean?>

    /**
     * Gets whether or not the given [userId] has registered for export via the credential exchange
     * protocol.
     */
    fun getVaultRegisteredForExport(userId: String): Boolean?

    /**
     * Stores the given value for whether or not the given [userId] has registered for export via
     * the credential exchange protocol.
     */
    fun storeVaultRegisteredForExport(userId: String, isRegistered: Boolean?)

    /**
     * Emits updates that track [getVaultRegisteredForExport] for the given [userId].
     */
    fun getVaultRegisteredForExportFlow(userId: String): Flow<Boolean?>

    /**
     * Gets the number of qualifying add cipher actions for the device.
     */
    fun getAddCipherActionCount(): Int?

    /**
     * Stores the given [count] completed "add" cipher actions taken place on the device.
     */
    fun storeAddCipherActionCount(count: Int?)

    /**
     * Gets the number of qualifying generated result actions for the device.
     */
    fun getGeneratedResultActionCount(): Int?

    /**
     * Stores the given [count] completed generated password or username result actions taken
     * for the device.
     */
    fun storeGeneratedResultActionCount(count: Int?)

    /**
     * Gets the number of qualifying create send actions for the device.
     */
    fun getCreateSendActionCount(): Int?

    /**
     * Stores the given [count] completed create send actions for the device.
     */
    fun storeCreateSendActionCount(count: Int?)
}
