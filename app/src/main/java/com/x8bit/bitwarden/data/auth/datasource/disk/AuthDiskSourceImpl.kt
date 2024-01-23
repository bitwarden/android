package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseEncryptedDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseEncryptedDiskSource.Companion.ENCRYPTED_BASE_KEY
import com.x8bit.bitwarden.data.platform.datasource.disk.legacy.LegacySecureStorageMigrator
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private const val USER_AUTO_UNLOCK_KEY_KEY = "$ENCRYPTED_BASE_KEY:userKeyAutoUnlock"
private const val UNIQUE_APP_ID_KEY = "$BASE_KEY:appId"
private const val REMEMBERED_EMAIL_ADDRESS_KEY = "$BASE_KEY:rememberedEmail"
private const val STATE_KEY = "$BASE_KEY:state"
private const val LAST_ACTIVE_TIME_KEY = "$BASE_KEY:lastActiveTime"
private const val INVALID_UNLOCK_ATTEMPTS_KEY = "$BASE_KEY:invalidUnlockAttempts"
private const val MASTER_KEY_ENCRYPTION_USER_KEY = "$BASE_KEY:masterKeyEncryptedUserKey"
private const val MASTER_KEY_ENCRYPTION_PRIVATE_KEY = "$BASE_KEY:encPrivateKey"
private const val PIN_PROTECTED_USER_KEY_KEY = "$BASE_KEY:pinKeyEncryptedUserKey"
private const val ENCRYPTED_PIN_KEY = "$BASE_KEY:protectedPin"
private const val ORGANIZATIONS_KEY = "$BASE_KEY:organizations"
private const val ORGANIZATION_KEYS_KEY = "$BASE_KEY:encOrgKeys"

/**
 * Primary implementation of [AuthDiskSource].
 */
@Suppress("TooManyFunctions")
class AuthDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
    legacySecureStorageMigrator: LegacySecureStorageMigrator,
    private val json: Json,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    AuthDiskSource {

    init {
        // We must migrate if necessary before any of the migrated values would be initialized
        // and accessed.
        legacySecureStorageMigrator.migrateIfNecessary()
    }

    private val inMemoryPinProtectedUserKeys = mutableMapOf<String, String?>()
    private val mutableOrganizationsFlowMap =
        mutableMapOf<String, MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?>>()

    override val uniqueAppId: String
        get() = getString(key = UNIQUE_APP_ID_KEY) ?: generateAndStoreUniqueAppId()

    override var rememberedEmailAddress: String?
        get() = getString(key = REMEMBERED_EMAIL_ADDRESS_KEY)
        set(value) {
            putString(
                key = REMEMBERED_EMAIL_ADDRESS_KEY,
                value = value,
            )
        }

    override var userState: UserStateJson?
        get() = getString(key = STATE_KEY)?.let { json.decodeFromString(it) }
        set(value) {
            putString(
                key = STATE_KEY,
                value = value?.let { json.encodeToString(value) },
            )
            mutableUserStateFlow.tryEmit(value)
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow
            .onSubscription { emit(userState) }

    private val mutableUserStateFlow = bufferedMutableSharedFlow<UserStateJson?>(replay = 1)

    override fun clearData(userId: String) {
        storeLastActiveTimeMillis(userId = userId, lastActiveTimeMillis = null)
        storeInvalidUnlockAttempts(userId = userId, invalidUnlockAttempts = null)
        storeUserKey(userId = userId, userKey = null)
        storeUserAutoUnlockKey(userId = userId, userAutoUnlockKey = null)
        storePinProtectedUserKey(userId = userId, pinProtectedUserKey = null)
        storeEncryptedPin(userId = userId, encryptedPin = null)
        storePrivateKey(userId = userId, privateKey = null)
        storeOrganizationKeys(userId = userId, organizationKeys = null)
        storeOrganizations(userId = userId, organizations = null)
    }

    override fun getLastActiveTimeMillis(userId: String): Long? =
        getLong(key = "${LAST_ACTIVE_TIME_KEY}_$userId")

    override fun storeLastActiveTimeMillis(
        userId: String,
        lastActiveTimeMillis: Long?,
    ) {
        putLong(
            key = "${LAST_ACTIVE_TIME_KEY}_$userId",
            value = lastActiveTimeMillis,
        )
    }

    override fun getInvalidUnlockAttempts(userId: String): Int? =
        getInt(key = "${INVALID_UNLOCK_ATTEMPTS_KEY}_$userId")

    override fun storeInvalidUnlockAttempts(
        userId: String,
        invalidUnlockAttempts: Int?,
    ) {
        putInt(
            key = "${INVALID_UNLOCK_ATTEMPTS_KEY}_$userId",
            value = invalidUnlockAttempts,
        )
    }

    override fun getUserKey(userId: String): String? =
        getString(key = "${MASTER_KEY_ENCRYPTION_USER_KEY}_$userId")

    override fun storeUserKey(userId: String, userKey: String?) {
        putString(
            key = "${MASTER_KEY_ENCRYPTION_USER_KEY}_$userId",
            value = userKey,
        )
    }

    override fun getPrivateKey(userId: String): String? =
        getString(key = "${MASTER_KEY_ENCRYPTION_PRIVATE_KEY}_$userId")

    override fun storePrivateKey(userId: String, privateKey: String?) {
        putString(
            key = "${MASTER_KEY_ENCRYPTION_PRIVATE_KEY}_$userId",
            value = privateKey,
        )
    }

    override fun getUserAutoUnlockKey(userId: String): String? =
        getEncryptedString(
            key = "${USER_AUTO_UNLOCK_KEY_KEY}_$userId",
            default = null,
        )

    override fun storeUserAutoUnlockKey(
        userId: String,
        userAutoUnlockKey: String?,
    ) {
        putEncryptedString(
            key = "${USER_AUTO_UNLOCK_KEY_KEY}_$userId",
            value = userAutoUnlockKey,
        )
    }

    override fun getPinProtectedUserKey(userId: String): String? =
        inMemoryPinProtectedUserKeys[userId]
            ?: getString(key = "${PIN_PROTECTED_USER_KEY_KEY}_$userId")

    override fun storePinProtectedUserKey(
        userId: String,
        pinProtectedUserKey: String?,
        inMemoryOnly: Boolean,
    ) {
        inMemoryPinProtectedUserKeys[userId] = pinProtectedUserKey
        if (inMemoryOnly) return
        putString(
            key = "${PIN_PROTECTED_USER_KEY_KEY}_$userId",
            value = pinProtectedUserKey,
        )
    }

    override fun getEncryptedPin(userId: String): String? =
        getString(key = "${ENCRYPTED_PIN_KEY}_$userId")

    override fun storeEncryptedPin(
        userId: String,
        encryptedPin: String?,
    ) {
        putString(
            key = "${ENCRYPTED_PIN_KEY}_$userId",
            value = encryptedPin,
        )
    }

    override fun getOrganizationKeys(userId: String): Map<String, String>? =
        getString(key = "${ORGANIZATION_KEYS_KEY}_$userId")
            ?.let { json.decodeFromString(it) }

    override fun storeOrganizationKeys(
        userId: String,
        organizationKeys: Map<String, String>?,
    ) {
        putString(
            key = "${ORGANIZATION_KEYS_KEY}_$userId",
            value = organizationKeys?.let { json.encodeToString(it) },
        )
    }

    override fun getOrganizations(
        userId: String,
    ): List<SyncResponseJson.Profile.Organization>? =
        getString(key = "${ORGANIZATIONS_KEY}_$userId")
            ?.let {
                // The organizations are stored as a map
                val organizationMap: Map<String, SyncResponseJson.Profile.Organization> =
                    json.decodeFromString(it)
                organizationMap.values.toList()
            }

    override fun getOrganizationsFlow(
        userId: String,
    ): Flow<List<SyncResponseJson.Profile.Organization>?> =
        getMutableOrganizationsFlow(userId = userId)
            .onSubscription { emit(getOrganizations(userId = userId)) }

    override fun storeOrganizations(
        userId: String,
        organizations: List<SyncResponseJson.Profile.Organization>?,
    ) {
        putString(
            key = "${ORGANIZATIONS_KEY}_$userId",
            value = organizations?.let { nonNullOrganizations ->
                // The organizations are stored as a map
                val organizationsMap = nonNullOrganizations.associateBy { it.id }
                json.encodeToString(organizationsMap)
            },
        )
        getMutableOrganizationsFlow(userId = userId).tryEmit(organizations)
    }

    private fun generateAndStoreUniqueAppId(): String =
        UUID
            .randomUUID()
            .toString()
            .also {
                putString(key = UNIQUE_APP_ID_KEY, value = it)
            }

    private fun getMutableOrganizationsFlow(
        userId: String,
    ): MutableSharedFlow<List<SyncResponseJson.Profile.Organization>?> =
        mutableOrganizationsFlowMap.getOrPut(userId) {
            bufferedMutableSharedFlow(replay = 1)
        }
}
