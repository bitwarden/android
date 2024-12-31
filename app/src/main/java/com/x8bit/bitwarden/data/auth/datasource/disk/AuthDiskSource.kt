package com.x8bit.bitwarden.data.auth.datasource.disk

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information.
 */
@Suppress("TooManyFunctions")
interface AuthDiskSource {

    /**
     * The currently persisted authenticator sync symmetric key. This key is used for
     * encrypting IPC traffic.
     */
    var authenticatorSyncSymmetricKey: ByteArray?

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
     * Get the authenticator sync unlock key. Null means there is no key, which means the user
     * has not enabled authenticator syncing
     */
    fun getAuthenticatorSyncUnlockKey(userId: String): String?

    /**
     * Store the authenticator sync unlock key. Storing a null key effectively disables
     * authenticator syncing.
     */
    fun storeAuthenticatorSyncUnlockKey(userId: String, authenticatorSyncUnlockKey: String?)

    /**
     * Retrieves the state indicating that the user should use a key connector.
     */
    fun getShouldUseKeyConnector(userId: String): Boolean?

    /**
     * Retrieves the state indicating that the user should use a key connector as a flow.
     */
    fun getShouldUseKeyConnectorFlow(userId: String): Flow<Boolean?>

    /**
     * Stores the boolean indicating that the user should use a key connector.
     */
    fun storeShouldUseKeyConnector(userId: String, shouldUseKeyConnector: Boolean?)

    /**
     * Retrieves the state indicating that the user has completed login with TDE.
     */
    fun getIsTdeLoginComplete(userId: String): Boolean?

    /**
     * Stores the boolean indicating that the user has completed login with TDE.
     */
    fun storeIsTdeLoginComplete(userId: String, isTdeLoginComplete: Boolean?)

    /**
     * Retrieves the state indicating that the user has chosen to trust this device.
     *
     * Note: This indicates intent to trust the device, the device may not be trusted yet.
     */
    fun getShouldTrustDevice(userId: String): Boolean?

    /**
     * Stores the boolean indicating that the user has chosen to trust this device for the given
     * [userId].
     *
     * Note: This indicates intent to trust the device, the device may not be trusted yet.
     */
    fun storeShouldTrustDevice(userId: String, shouldTrustDevice: Boolean?)

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
     * Gets the device key for the given [userId].
     */
    fun getDeviceKey(userId: String): String?

    /**
     * Stores the device key for the given [userId].
     */
    fun storeDeviceKey(userId: String, deviceKey: String?)

    /**
     * Gets the stored [PendingAuthRequestJson] for the given [userId].
     */
    fun getPendingAuthRequest(userId: String): PendingAuthRequestJson?

    /**
     * Stores the [PendingAuthRequestJson] for the given [userId].
     */
    fun storePendingAuthRequest(
        userId: String,
        pendingAuthRequest: PendingAuthRequestJson?,
    )

    /**
     * Gets the biometrics key for the given [userId].
     */
    fun getUserBiometricUnlockKey(userId: String): String?

    /**
     * Stores the biometrics key for the given [userId].
     */
    fun storeUserBiometricUnlockKey(userId: String, biometricsKey: String?)

    /**
     * Gets the flow for the biometrics key for the given [userId].
     */
    fun getUserBiometicUnlockKeyFlow(userId: String): Flow<String?>

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
     * Retrieves a flow for the pin-protected user key for the given [userId].
     */
    fun getPinProtectedUserKeyFlow(userId: String): Flow<String?>

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
     * Stores the organization data for the given [userId].
     */
    fun storeOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    )

    /**
     * Gets the master password hash for the given [userId].
     */
    fun getMasterPasswordHash(userId: String): String?

    /**
     * Stores the [passwordHash] for the given [userId].
     */
    fun storeMasterPasswordHash(userId: String, passwordHash: String?)

    /**
     * Gets the policies for the given [userId].
     */
    fun getPolicies(userId: String): List<SyncResponseJson.Policy>?

    /**
     * Emits updates that track [getPolicies]. This will replay the last known value, if any.
     */
    fun getPoliciesFlow(userId: String): Flow<List<SyncResponseJson.Policy>?>

    /**
     * Stores the [policies] for the given [userId].
     */
    fun storePolicies(userId: String, policies: List<SyncResponseJson.Policy>?)

    /**
     * Gets the account tokens for the given [userId].
     */
    fun getAccountTokens(userId: String): AccountTokensJson?

    /**
     * Emits updates that track [getAccountTokens]. This will replay the last known value, if any.
     */
    fun getAccountTokensFlow(userId: String): Flow<AccountTokensJson?>

    /**
     * Stores the [accountTokens] for the given [userId].
     */
    fun storeAccountTokens(userId: String, accountTokens: AccountTokensJson?)

    /**
     * Gets the onboarding status for the given [userId].
     */
    fun getOnboardingStatus(userId: String): OnboardingStatus?

    /**
     * Stores the [onboardingStatus] for the given [userId].
     */
    fun storeOnboardingStatus(userId: String, onboardingStatus: OnboardingStatus?)

    /**
     *  Emits updates that track [getOnboardingStatus]. This will replay the last known value,
     *  if any exists.
     */
    fun getOnboardingStatusFlow(userId: String): Flow<OnboardingStatus?>

    /**
     * Gets the show import logins flag for the given [userId].
     */
    fun getShowImportLogins(userId: String): Boolean?

    /**
     * Stores the show import logins flag for the given [userId].
     */
    fun storeShowImportLogins(userId: String, showImportLogins: Boolean?)

    /**
     * Emits updates that track [getShowImportLogins]. This will replay the last known value.
     */
    fun getShowImportLoginsFlow(userId: String): Flow<Boolean?>

    /**
     * Gets the new device notice state for the given [userId].
     */
    fun getNewDeviceNoticeState(userId: String): NewDeviceNoticeState

    /**
     * Stores the new device notice state for the given [userId].
     */
    fun storeNewDeviceNoticeState(userId: String, newState: NewDeviceNoticeState?)
}
