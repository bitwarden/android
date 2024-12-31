package com.x8bit.bitwarden.data.auth.datasource.disk.util

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeDisplayStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.NewDeviceNoticeState
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.PendingAuthRequestJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import org.junit.Assert.assertEquals

class FakeAuthDiskSource : AuthDiskSource {

    override var authenticatorSyncSymmetricKey: ByteArray? = null

    override val uniqueAppId: String = "testUniqueAppId"

    override var rememberedEmailAddress: String? = null
    override var rememberedOrgIdentifier: String? = null

    private val mutableShouldUseKeyConnectorFlowMap =
        mutableMapOf<String, MutableSharedFlow<Boolean?>>()
    private val mutableOrganizationsFlowMap =
        mutableMapOf<String, MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?>>()
    private val mutablePoliciesFlowMap =
        mutableMapOf<String, MutableSharedFlow<List<SyncResponseJson.Policy>?>>()
    private val mutableAccountTokensFlowMap =
        mutableMapOf<String, MutableSharedFlow<AccountTokensJson?>>()
    private val mutableShowImportLoginsFlowMap = mutableMapOf<String, MutableSharedFlow<Boolean?>>()
    private val mutableBiometricKeysFlowMap = mutableMapOf<String, MutableSharedFlow<String?>>()
    private val mutablePinProtectedUserKeysFlowMap =
        mutableMapOf<String, MutableSharedFlow<String?>>()

    private val mutableOnboardingStatusFlowMap =
        mutableMapOf<String, MutableSharedFlow<OnboardingStatus?>>()
    private val mutableUserStateFlow = bufferedMutableSharedFlow<UserStateJson?>(replay = 1)

    private val storedShouldUseKeyConnector = mutableMapOf<String, Boolean?>()
    private val storedIsTdeLoginComplete = mutableMapOf<String, Boolean?>()
    private val storedShouldTrustDevice = mutableMapOf<String, Boolean?>()
    private val storedInvalidUnlockAttempts = mutableMapOf<String, Int?>()
    private val storedUserKeys = mutableMapOf<String, String?>()
    private val storedPrivateKeys = mutableMapOf<String, String?>()
    private val storedTwoFactorTokens = mutableMapOf<String, String?>()
    private val storedUserAutoUnlockKeys = mutableMapOf<String, String?>()
    private val storedPinProtectedUserKeys = mutableMapOf<String, Pair<String?, Boolean>>()
    private val storedEncryptedPins = mutableMapOf<String, String?>()
    private val storedOrganizations =
        mutableMapOf<String, List<SyncResponseJson.Profile.Organization>?>()
    private val storedOrganizationKeys = mutableMapOf<String, Map<String, String>?>()
    private val storedAccountTokens = mutableMapOf<String, AccountTokensJson?>()
    private val storedDeviceKey = mutableMapOf<String, String?>()
    private val storedPendingAuthRequests = mutableMapOf<String, PendingAuthRequestJson?>()
    private val storedBiometricKeys = mutableMapOf<String, String?>()
    private val storedMasterPasswordHashes = mutableMapOf<String, String?>()
    private val storedAuthenticationSyncKeys = mutableMapOf<String, String?>()
    private val storedPolicies = mutableMapOf<String, List<SyncResponseJson.Policy>?>()
    private val storedOnboardingStatus = mutableMapOf<String, OnboardingStatus?>()
    private val storedShowImportLogins = mutableMapOf<String, Boolean?>()
    private val storedNewDeviceNoticeState = mutableMapOf<String, NewDeviceNoticeState?>()

    override var userState: UserStateJson? = null
        set(value) {
            field = value
            mutableUserStateFlow.tryEmit(value)
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow.onSubscription { emit(userState) }

    override fun clearData(userId: String) {
        storedInvalidUnlockAttempts.remove(userId)
        storedUserKeys.remove(userId)
        storedPrivateKeys.remove(userId)
        storedTwoFactorTokens.clear()
        storedUserAutoUnlockKeys.remove(userId)
        storedPinProtectedUserKeys.remove(userId)
        storedEncryptedPins.remove(userId)
        storedOrganizations.remove(userId)
        storedPolicies.remove(userId)
        storedAccountTokens.remove(userId)
        storedBiometricKeys.remove(userId)
        storedOrganizationKeys.remove(userId)

        mutableShouldUseKeyConnectorFlowMap.remove(userId)
        mutableOrganizationsFlowMap.remove(userId)
        mutablePoliciesFlowMap.remove(userId)
        mutableAccountTokensFlowMap.remove(userId)
    }

    override fun getShouldUseKeyConnectorFlow(
        userId: String,
    ): Flow<Boolean?> = getMutableShouldUseKeyConnectorFlow(userId = userId)
        .onSubscription { emit(getShouldUseKeyConnector(userId = userId)) }

    override fun getShouldUseKeyConnector(
        userId: String,
    ): Boolean? = storedShouldUseKeyConnector[userId]

    override fun storeShouldUseKeyConnector(userId: String, shouldUseKeyConnector: Boolean?) {
        storedShouldUseKeyConnector[userId] = shouldUseKeyConnector
        getMutableShouldUseKeyConnectorFlow(userId = userId).tryEmit(shouldUseKeyConnector)
    }

    override fun getIsTdeLoginComplete(userId: String): Boolean? = storedIsTdeLoginComplete[userId]

    override fun storeIsTdeLoginComplete(userId: String, isTdeLoginComplete: Boolean?) {
        storedIsTdeLoginComplete[userId] = isTdeLoginComplete
    }

    override fun getShouldTrustDevice(userId: String): Boolean? =
        storedShouldTrustDevice[userId]

    override fun storeShouldTrustDevice(userId: String, shouldTrustDevice: Boolean?) {
        storedShouldTrustDevice[userId] = shouldTrustDevice
    }

    override fun getInvalidUnlockAttempts(userId: String): Int? =
        storedInvalidUnlockAttempts[userId]

    override fun storeInvalidUnlockAttempts(
        userId: String,
        invalidUnlockAttempts: Int?,
    ) {
        storedInvalidUnlockAttempts[userId] = invalidUnlockAttempts
    }

    override fun getUserKey(userId: String): String? = storedUserKeys[userId]

    override fun storeUserKey(userId: String, userKey: String?) {
        storedUserKeys[userId] = userKey
    }

    override fun getPrivateKey(userId: String): String? = storedPrivateKeys[userId]

    override fun storePrivateKey(userId: String, privateKey: String?) {
        storedPrivateKeys[userId] = privateKey
    }

    override fun getTwoFactorToken(email: String): String? = storedTwoFactorTokens[email]

    override fun storeTwoFactorToken(email: String, twoFactorToken: String?) {
        storedTwoFactorTokens[email] = twoFactorToken
    }

    override fun getUserAutoUnlockKey(userId: String): String? =
        storedUserAutoUnlockKeys[userId]

    override fun storeUserAutoUnlockKey(userId: String, userAutoUnlockKey: String?) {
        storedUserAutoUnlockKeys[userId] = userAutoUnlockKey
    }

    override fun getPinProtectedUserKey(userId: String): String? =
        storedPinProtectedUserKeys[userId]?.first

    override fun storePinProtectedUserKey(
        userId: String,
        pinProtectedUserKey: String?,
        inMemoryOnly: Boolean,
    ) {
        storedPinProtectedUserKeys[userId] = pinProtectedUserKey to inMemoryOnly
        getMutablePinProtectedUserKeyFlow(userId).tryEmit(pinProtectedUserKey)
    }

    override fun getPinProtectedUserKeyFlow(userId: String): Flow<String?> =
        getMutablePinProtectedUserKeyFlow(userId)
            .onSubscription {
                emit(getPinProtectedUserKey(userId))
            }

    override fun getEncryptedPin(userId: String): String? =
        storedEncryptedPins[userId]

    override fun storeEncryptedPin(userId: String, encryptedPin: String?) {
        storedEncryptedPins[userId] = encryptedPin
    }

    override fun getOrganizationKeys(
        userId: String,
    ): Map<String, String>? = storedOrganizationKeys[userId]

    override fun storeOrganizationKeys(
        userId: String,
        organizationKeys: Map<String, String>?,
    ) {
        storedOrganizationKeys[userId] = organizationKeys
    }

    override fun getOrganizations(
        userId: String,
    ): List<SyncResponseJson.Profile.Organization>? = storedOrganizations[userId]

    override fun getOrganizationsFlow(
        userId: String,
    ): Flow<List<SyncResponseJson.Profile.Organization>?> =
        getMutableOrganizationsFlow(userId).onSubscription { emit(getOrganizations(userId)) }

    override fun storeOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    ) {
        storedOrganizations[userId] = organizations
        getMutableOrganizationsFlow(userId = userId).tryEmit(organizations)
    }

    override fun getDeviceKey(userId: String): String? = storedDeviceKey[userId]

    override fun storeDeviceKey(userId: String, deviceKey: String?) {
        storedDeviceKey[userId] = deviceKey
    }

    override fun getPendingAuthRequest(userId: String): PendingAuthRequestJson? =
        storedPendingAuthRequests[userId]

    override fun storePendingAuthRequest(
        userId: String,
        pendingAuthRequest: PendingAuthRequestJson?,
    ) {
        storedPendingAuthRequests[userId] = pendingAuthRequest
    }

    override fun getUserBiometricUnlockKey(userId: String): String? =
        storedBiometricKeys[userId]

    override fun storeUserBiometricUnlockKey(userId: String, biometricsKey: String?) {
        storedBiometricKeys[userId] = biometricsKey
        getMutableBiometricUnlockKeyFlow(userId).tryEmit(biometricsKey)
    }

    override fun getUserBiometicUnlockKeyFlow(userId: String): Flow<String?> =
        getMutableBiometricUnlockKeyFlow(userId)
            .onSubscription { emit(getUserBiometricUnlockKey(userId)) }

    override fun getMasterPasswordHash(userId: String): String? =
        storedMasterPasswordHashes[userId]

    override fun storeMasterPasswordHash(userId: String, passwordHash: String?) {
        storedMasterPasswordHashes[userId] = passwordHash
    }

    override fun getAuthenticatorSyncUnlockKey(userId: String): String? =
        storedAuthenticationSyncKeys[userId]

    override fun storeAuthenticatorSyncUnlockKey(
        userId: String,
        authenticatorSyncUnlockKey: String?,
    ) {
        storedAuthenticationSyncKeys[userId] = authenticatorSyncUnlockKey
    }

    override fun getPolicies(
        userId: String,
    ): List<SyncResponseJson.Policy>? = storedPolicies[userId]

    override fun getPoliciesFlow(userId: String): Flow<List<SyncResponseJson.Policy>?> =
        getMutablePoliciesFlow(userId = userId).onSubscription { emit(getPolicies(userId)) }

    override fun storePolicies(userId: String, policies: List<SyncResponseJson.Policy>?) {
        storedPolicies[userId] = policies
        getMutablePoliciesFlow(userId = userId).tryEmit(policies)
    }

    override fun getAccountTokens(userId: String): AccountTokensJson? =
        storedAccountTokens[userId]

    override fun getAccountTokensFlow(userId: String): Flow<AccountTokensJson?> =
        getMutableAccountTokensFlow(userId = userId)
            .onSubscription { emit(getAccountTokens(userId)) }

    override fun storeAccountTokens(userId: String, accountTokens: AccountTokensJson?) {
        storedAccountTokens[userId] = accountTokens
        getMutableAccountTokensFlow(userId = userId).tryEmit(accountTokens)
    }

    override fun getOnboardingStatus(userId: String): OnboardingStatus? =
        storedOnboardingStatus[userId]

    override fun storeOnboardingStatus(userId: String, onboardingStatus: OnboardingStatus?) {
        storedOnboardingStatus[userId] = onboardingStatus
        getMutableOnboardingStatusFlow(userId = userId).tryEmit(onboardingStatus)
    }

    override fun getOnboardingStatusFlow(userId: String): Flow<OnboardingStatus?> =
        getMutableOnboardingStatusFlow(userId = userId)
            .onSubscription { emit(getOnboardingStatus(userId)) }

    override fun getShowImportLogins(userId: String): Boolean? =
        storedShowImportLogins[userId]

    override fun storeShowImportLogins(userId: String, showImportLogins: Boolean?) {
        storedShowImportLogins[userId] = showImportLogins
        getMutableShowImportLoginsFlow(userId = userId).tryEmit(showImportLogins)
    }

    override fun getShowImportLoginsFlow(userId: String): Flow<Boolean?> =
        getMutableShowImportLoginsFlow(userId)
            .onSubscription { emit(getShowImportLogins(userId)) }

    override fun getNewDeviceNoticeState(userId: String): NewDeviceNoticeState {
        return storedNewDeviceNoticeState[userId] ?: NewDeviceNoticeState(
            displayStatus = NewDeviceNoticeDisplayStatus.HAS_NOT_SEEN,
            lastSeenDate = null,
            )
    }

    override fun storeNewDeviceNoticeState(userId: String, newState: NewDeviceNoticeState?) {
        storedNewDeviceNoticeState[userId] = newState
    }

    /**
     * Assert the the [isTdeLoginComplete] was stored successfully using the [userId].
     */
    fun assertIsTdeLoginComplete(userId: String, isTdeLoginComplete: Boolean?) {
        assertEquals(isTdeLoginComplete, storedIsTdeLoginComplete[userId])
    }

    /**
     * Assert the the [shouldTrustDevice] was stored successfully using the [userId].
     */
    fun assertShouldTrustDevice(userId: String, shouldTrustDevice: Boolean?) {
        assertEquals(shouldTrustDevice, storedShouldTrustDevice[userId])
    }

    /**
     * Assert the the [shouldUseKeyConnector] was stored successfully using the [userId].
     */
    fun assertShouldUseKeyConnector(userId: String, shouldUseKeyConnector: Boolean?) {
        assertEquals(shouldUseKeyConnector, storedShouldUseKeyConnector[userId])
    }

    /**
     * Assert that the given [userState] matches the currently tracked value.
     */
    fun assertUserState(userState: UserStateJson) {
        assertEquals(userState, this.userState)
    }

    /**
     * Assert that the [accountTokens] was stored successfully using the [userId].
     */
    fun assertAccountTokens(userId: String, accountTokens: AccountTokensJson?) {
        assertEquals(accountTokens, this.storedAccountTokens[userId])
    }

    /**
     * Assert that the [invalidUnlockAttempts] was stored successfully using the [userId].
     */
    fun assertInvalidUnlockAttempts(userId: String, invalidUnlockAttempts: Int?) {
        assertEquals(invalidUnlockAttempts, storedInvalidUnlockAttempts[userId])
    }

    /**
     * Assert that the [userKey] was stored successfully using the [userId].
     */
    fun assertUserKey(userId: String, userKey: String?) {
        assertEquals(userKey, storedUserKeys[userId])
    }

    /**
     * Assert that the [privateKey] was stored successfully using the [userId].
     */
    fun assertPrivateKey(userId: String, privateKey: String?) {
        assertEquals(privateKey, storedPrivateKeys[userId])
    }

    /**
     * Assert that the [twoFactorToken] was stored successfully using the [email].
     */
    fun assertTwoFactorToken(email: String, twoFactorToken: String?) {
        assertEquals(twoFactorToken, storedTwoFactorTokens[email])
    }

    /**
     * Assert that the [userAutoUnlockKey] was stored successfully using the [userId].
     */
    fun assertUserAutoUnlockKey(userId: String, userAutoUnlockKey: String?) {
        assertEquals(userAutoUnlockKey, storedUserAutoUnlockKeys[userId])
    }

    /**
     * Assert that the [encryptedPin] was stored successfully using the [userId].
     */
    fun assertEncryptedPin(userId: String, encryptedPin: String?) {
        assertEquals(encryptedPin, storedEncryptedPins[userId])
    }

    /**
     * Assert that the [pinProtectedUserKey] was stored successfully using the [userId].
     */
    fun assertPinProtectedUserKey(
        userId: String,
        pinProtectedUserKey: String?,
        inMemoryOnly: Boolean = false,
    ) {
        assertEquals(pinProtectedUserKey to inMemoryOnly, storedPinProtectedUserKeys[userId])
    }

    /**
     * Assert the the [organizationKeys] was stored successfully using the [userId].
     */
    fun assertOrganizationKeys(userId: String, organizationKeys: Map<String, String>?) {
        assertEquals(organizationKeys, storedOrganizationKeys[userId])
    }

    /**
     * Assert that the [deviceKey] was stored successfully using the [userId].
     */
    fun assertDeviceKey(userId: String, deviceKey: String?) {
        assertEquals(deviceKey, storedDeviceKey[userId])
    }

    /**
     * Assert that the [pendingAuthRequest] was stored successfully using the [userId].
     */
    fun assertPendingAuthRequest(userId: String, pendingAuthRequest: PendingAuthRequestJson?) {
        assertEquals(pendingAuthRequest, storedPendingAuthRequests[userId])
    }

    /**
     * Assert that the [biometricsKey] was stored successfully using the [userId].
     */
    fun assertBiometricsKey(userId: String, biometricsKey: String?) {
        assertEquals(biometricsKey, storedBiometricKeys[userId])
    }

    /**
     * Assert that the [passwordHash] was stored successfully using the [userId].
     */
    fun assertMasterPasswordHash(userId: String, passwordHash: String?) {
        assertEquals(passwordHash, storedMasterPasswordHashes[userId])
    }

    /**
     * Assert that the [organizations] were stored successfully using the [userId].
     */
    fun assertOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    ) {
        assertEquals(organizations, storedOrganizations[userId])
    }

    /**
     * Assert that the [policies] were stored successfully using the [userId].
     */
    fun assertPolicies(
        userId: String,
        policies: List<SyncResponseJson.Policy>?,
    ) {
        assertEquals(policies, storedPolicies[userId])
    }

    //region Private helper functions

    private fun getMutableShouldUseKeyConnectorFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> =
        mutableShouldUseKeyConnectorFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableOrganizationsFlow(
        userId: String,
    ): MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?> =
        mutableOrganizationsFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutablePoliciesFlow(
        userId: String,
    ): MutableSharedFlow<List<SyncResponseJson.Policy>?> =
        mutablePoliciesFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableAccountTokensFlow(
        userId: String,
    ): MutableSharedFlow<AccountTokensJson?> =
        mutableAccountTokensFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableOnboardingStatusFlow(
        userId: String,
    ): MutableSharedFlow<OnboardingStatus?> =
        mutableOnboardingStatusFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }

    private fun getMutableShowImportLoginsFlow(
        userId: String,
    ): MutableSharedFlow<Boolean?> = mutableShowImportLoginsFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    private fun getMutableBiometricUnlockKeyFlow(
        userId: String,
    ): MutableSharedFlow<String?> = mutableBiometricKeysFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    private fun getMutablePinProtectedUserKeyFlow(
        userId: String,
    ): MutableSharedFlow<String?> = mutablePinProtectedUserKeysFlowMap.getOrPut(userId) {
        bufferedMutableSharedFlow(replay = 1)
    }

    //endregion Private helper functions
}
