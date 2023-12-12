package com.x8bit.bitwarden.data.auth.datasource.disk.util

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import org.junit.Assert.assertEquals

class FakeAuthDiskSource : AuthDiskSource {

    override val uniqueAppId: String = "testUniqueAppId"

    override var rememberedEmailAddress: String? = null

    private val mutableUserStateFlow =
        MutableSharedFlow<UserStateJson?>(
            replay = 1,
            extraBufferCapacity = Int.MAX_VALUE,
        )

    private val storedUserKeys = mutableMapOf<String, String?>()
    private val storedPrivateKeys = mutableMapOf<String, String?>()
    private val storedOrganizationKeys = mutableMapOf<String, Map<String, String>?>()

    override var userState: UserStateJson? = null
        set(value) {
            field = value
            mutableUserStateFlow.tryEmit(value)
        }

    override val userStateFlow: Flow<UserStateJson?>
        get() = mutableUserStateFlow.onSubscription { emit(userState) }

    override fun getUserKey(userId: String): String? = storedUserKeys[userId]

    override fun storeUserKey(userId: String, userKey: String?) {
        storedUserKeys[userId] = userKey
    }

    override fun getPrivateKey(userId: String): String? = storedPrivateKeys[userId]

    override fun storePrivateKey(userId: String, privateKey: String?) {
        storedPrivateKeys[userId] = privateKey
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

    /**
     * Assert that the given [userState] matches the currently tracked value.
     */
    fun assertUserState(userState: UserStateJson) {
        assertEquals(userState, this.userState)
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
     * Assert the the [organizationKeys] was stored successfully using the [userId].
     */
    fun assertOrganizationKeys(userId: String, organizationKeys: Map<String, String>?) {
        assertEquals(organizationKeys, storedOrganizationKeys[userId])
    }
}
