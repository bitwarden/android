package com.x8bit.bitwarden.data.auth.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val REMEMBERED_EMAIL_ADDRESS_KEY = "$BASE_KEY:rememberedEmail"
private const val STATE_KEY = "$BASE_KEY:state"
private const val MASTER_KEY_ENCRYPTION_USER_KEY = "masterKeyEncryptedUserKey"
private const val MASTER_KEY_ENCRYPTION_PRIVATE_KEY = "encPrivateKey"

/**
 * Primary implementation of [AuthDiskSource].
 */
class AuthDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    AuthDiskSource {
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
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow
            .onSubscription { emit(userState) }

    private val mutableUserStateFlow = MutableSharedFlow<UserStateJson?>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
    )

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?,
    ) {
        when (key) {
            STATE_KEY -> mutableUserStateFlow.tryEmit(userState)
        }
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
}
