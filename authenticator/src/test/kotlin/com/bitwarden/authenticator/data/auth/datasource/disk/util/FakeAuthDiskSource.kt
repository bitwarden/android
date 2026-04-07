package com.bitwarden.authenticator.data.auth.datasource.disk.util

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

class FakeAuthDiskSource : AuthDiskSource {

    private var storedLastActiveTimeMillis: Long? = null
    private var storedUserBiometricUnlockKey: String? = null
    private var storedUserBiometricInitVector: ByteArray? = null
    private val mutableUserBiometricUnlockKeyFlow = bufferedMutableSharedFlow<String?>(replay = 1)

    override val uniqueAppId: String
        get() = UUID.randomUUID().toString()

    override fun getLastActiveTimeMillis(): Long? = storedLastActiveTimeMillis

    override fun storeLastActiveTimeMillis(lastActiveTimeMillis: Long?) {
        storedLastActiveTimeMillis = lastActiveTimeMillis
    }

    fun assertLastActiveTimeMillis(lastActiveTimeMillis: Long?) {
        assertEquals(lastActiveTimeMillis, storedLastActiveTimeMillis)
    }

    override fun getUserBiometricUnlockKey(): String? = storedUserBiometricUnlockKey

    override val userBiometricUnlockKeyFlow: Flow<String?>
        get() = mutableUserBiometricUnlockKeyFlow
            .onSubscription { emit(getUserBiometricUnlockKey()) }

    override fun storeUserBiometricUnlockKey(biometricsKey: String?) {
        mutableUserBiometricUnlockKeyFlow.tryEmit(biometricsKey)
        this@FakeAuthDiskSource.storedUserBiometricUnlockKey = biometricsKey
    }

    fun assertUserBiometricUnlockKey(biometricsKey: String?) {
        assertEquals(biometricsKey, storedUserBiometricUnlockKey)
    }

    override var authenticatorBridgeSymmetricSyncKey: ByteArray? = null

    override var userBiometricKeyInitVector: ByteArray?
        get() = storedUserBiometricInitVector
        set(value) {
            storedUserBiometricInitVector = value
        }

    fun assertUserBiometricKeyInitVector(iv: ByteArray?) {
        assertEquals(iv, storedUserBiometricInitVector)
    }
}
