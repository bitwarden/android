package com.x8bit.bitwarden.data.auth.datasource.disk

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information.
 */
@Suppress("TooManyFunctions")
interface AuthDiskSource {
    /**
     * Retrieves a unique ID for the application that is stored locally. This will generate a new
     * one if it does not yet exist and it will only be reset for new installs or when clearing
     * application data.
     */
    val uniqueAppId: String

    /**
     * The currently persisted saved email address (or `null` if not set).
     */
    var rememberedEmailAddress: String?

    /**
     * The currently persisted organization identifier (or `null` if not set).
     */
    var rememberedOrgIdentifier: String?

    /**
     * The currently persisted user state information (or `null` if not set).
     */
    var userState: UserStateJson?

    /**
     * Emits updates that track [userState]. This will replay the last known value, if any.
     */
    val userStateFlow: Flow<UserStateJson?>

    /**
     * Clears all the settings data for the given user.
     *
     * Note that this does not include any data saved in the [userState].
     */
    fun clearData(userId: String)

    /**
     * Retrieves the "last active time" for the given [userId], in milliseconds.
     *
     * This time is intended to be derived from a call to
     * [SystemClock.elapsedRealtime()](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime())
     */
    fun getLastActiveTimeMillis(userId: String): Long?

    /**
     * Stores the [lastActiveTimeMillis] for the given [userId].
     *
     * This time is intended to be derived from a call to
     * [SystemClock.elapsedRealtime()](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime())
     */
    fun storeLastActiveTimeMillis(
        userId: String,
        lastActiveTimeMillis: Long?,
    )

    /**
     * Retrieves the number of consecutive invalid lock attempts for the given [userId].
     */
    fun getInvalidUnlockAttempts(userId: String): Int?

    /**
     * Stores the number of consecutive invalid lock attempts for the given [userId].
     */
    fun storeInvalidUnlockAttempts(
        userId: String,
        invalidUnlockAttempts: Int?,
    )

    /**
     * Retrieves a user key using a [userId].
     */
    fun getUserKey(userId: String): String?

    /**
     * Stores a user key using a [userId].
     */
    fun storeUserKey(userId: String, userKey: String?)

    /**
     * Retrieves a private key using a [userId].
     */
    fun getPrivateKey(userId: String): String?

    /**
     * Stores a private key using a [userId].
     */
    fun storePrivateKey(userId: String, privateKey: String?)

    /**
     * Retrieves a user auto-unlock key for the given [userId].
     */
    fun getUserAutoUnlockKey(userId: String): String?

    /**
     * Stores a user auto-unlock key for the given [userId].
     */
    fun storeUserAutoUnlockKey(userId: String, userAutoUnlockKey: String?)

    /**
     * Gets the biometrics key for the given [userId].
     */
    fun getUserBiometricUnlockKey(userId: String): String?

    /**
     * Stores the biometrics key for the given [userId].
     */
    fun storeUserBiometricUnlockKey(userId: String, biometricsKey: String?)

    /**
     * Retrieves a pin-protected user key for the given [userId].
     */
    fun getPinProtectedUserKey(userId: String): String?

    /**
     * Stores a pin-protected user key for the given [userId].
     *
     * When [inMemoryOnly] is `true`, the value will only be available via a call to
     * [getPinProtectedUserKey] during the current app session.
     */
    fun storePinProtectedUserKey(
        userId: String,
        pinProtectedUserKey: String?,
        inMemoryOnly: Boolean = false,
    )

    /**
     * Gets a two-factor auth token using a user's [email].
     */
    fun getTwoFactorToken(email: String): String?

    /**
     * Stores a two-factor auth token using a user's [email].
     */
    fun storeTwoFactorToken(email: String, twoFactorToken: String?)

    /**
     * Retrieves an encrypted PIN for the given [userId].
     */
    fun getEncryptedPin(userId: String): String?

    /**
     * Stores an encrypted PIN for the given [userId].
     */
    fun storeEncryptedPin(userId: String, encryptedPin: String?)

    /**
     * Gets the organization keys for the given [userId] in the form of a mapping from organization
     * ID to encrypted organization key.
     */
    fun getOrganizationKeys(userId: String): Map<String, String>?

    /**
     * Stores the organization keys for the given [userId] in the form of a mapping from
     * organization ID to encrypted organization key.
     */
    fun storeOrganizationKeys(
        userId: String,
        organizationKeys: Map<String, String>?,
    )

    /**
     * Gets the organization data for the given [userId].
     */
    fun getOrganizations(userId: String): List<SyncResponseJson.Profile.Organization>?

    /**
     * Emits updates that track [getOrganizations]. This will replay the last known value, if any.
     */
    fun getOrganizationsFlow(userId: String): Flow<List<SyncResponseJson.Profile.Organization>?>

    /**
     * Gets the master password hash for the given [userId].
     */
    fun getMasterPasswordHash(userId: String): String?

    /**
     * Stores the [passwordHash] for the given [userId].
     */
    fun storeMasterPasswordHash(userId: String, passwordHash: String?)

    /**
     * Stores the organization data for the given [userId].
     */
    fun storeOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    )
}
