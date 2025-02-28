package com.bitwarden.authenticator.data.auth.datasource.disk.util

import com.bitwarden.authenticator.data.auth.datasource.disk.AuthDiskSource

class FakeAuthDiskSource : AuthDiskSource {

    private var lastActiveTimeMillis: Long? = null
    private var userBiometricUnlockKey: String? = null

    override fun getLastActiveTimeMillis(): Long? = lastActiveTimeMillis

    override fun storeLastActiveTimeMillis(lastActiveTimeMillis: Long?) {
        this@FakeAuthDiskSource.lastActiveTimeMillis = lastActiveTimeMillis
    }

    override fun getUserBiometricUnlockKey(): String? = userBiometricUnlockKey

    override fun storeUserBiometricUnlockKey(biometricsKey: String?) {
        this@FakeAuthDiskSource.userBiometricUnlockKey = biometricsKey
    }

    override var authenticatorBridgeSymmetricSyncKey: ByteArray? = null
}
