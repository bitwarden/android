package com.bitwarden.authenticator.data.auth.datasource.disk.util

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import java.util.UUID

class FakeAuthDiskSource : AuthDiskSource {

    private var lastActiveTimeMillis: Long? = null
    private var userBiometricUnlockKey: String? = null
    private val mutableUserBiometricUnlockKeyFlow = bufferedMutableSharedFlow<String?>(replay = 1)

    override val uniqueAppId: String
        get() = UUID.randomUUID().toString()

    override fun getLastActiveTimeMillis(): Long? = lastActiveTimeMillis

    override fun storeLastActiveTimeMillis(lastActiveTimeMillis: Long?) {
        this@FakeAuthDiskSource.lastActiveTimeMillis = lastActiveTimeMillis
    }

    override fun getUserBiometricUnlockKey(): String? = userBiometricUnlockKey

    override val userBiometricUnlockKeyFlow: Flow<String?>
        get() =
            mutableUserBiometricUnlockKeyFlow
                .onSubscription {
                    emit(getUserBiometricUnlockKey())
                }

    override fun storeUserBiometricUnlockKey(biometricsKey: String?) {
        mutableUserBiometricUnlockKeyFlow.tryEmit(biometricsKey)
        this@FakeAuthDiskSource.userBiometricUnlockKey = biometricsKey
    }

    override var authenticatorBridgeSymmetricSyncKey: ByteArray? = null
}
