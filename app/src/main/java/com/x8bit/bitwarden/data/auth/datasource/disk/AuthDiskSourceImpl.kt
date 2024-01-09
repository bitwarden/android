package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseEncryptedDiskSource
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private const val UNIQUE_APP_ID_KEY = "$BASE_KEY:appId"
private const val REMEMBERED_EMAIL_ADDRESS_KEY = "$BASE_KEY:rememberedEmail"
private const val STATE_KEY = "$BASE_KEY:state"
private const val MASTER_KEY_ENCRYPTION_USER_KEY = "$BASE_KEY:masterKeyEncryptedUserKey"
private const val MASTER_KEY_ENCRYPTION_PRIVATE_KEY = "$BASE_KEY:encPrivateKey"
private const val ORGANIZATIONS_KEY = "$BASE_KEY:organizations"
private const val ORGANIZATION_KEYS_KEY = "$BASE_KEY:encOrgKeys"

/**
 * Primary implementation of [AuthDiskSource].
 */
@Suppress("TooManyFunctions")
class AuthDiskSourceImpl(
    encryptedSharedPreferences: SharedPreferences,
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    AuthDiskSource {
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
