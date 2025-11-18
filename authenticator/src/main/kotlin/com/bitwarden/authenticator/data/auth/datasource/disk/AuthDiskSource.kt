package com.bitwarden.authenticator.data.auth.datasource.disk

import com.bitwarden.network.provider.AppIdProvider
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information.
 */
interface AuthDiskSource : AppIdProvider {

    /**
     * Tracks the biometrics key.
     */
    val userBiometricUnlockKeyFlow: Flow<String?>

    /**
     * Retrieves the "last active time".
     *
     * This time is intended to be derived from a call to
     * [SystemClock.elapsedRealtime()](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime())
     */
    fun getLastActiveTimeMillis(): Long?

    /**
     * Stores the [lastActiveTimeMillis] .
     *
     * This time is intended to be derived from a call to
     * [SystemClock.elapsedRealtime()](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime())
     */
    fun storeLastActiveTimeMillis(lastActiveTimeMillis: Long?)

    /**
     * Gets the biometrics key.
     */
    fun getUserBiometricUnlockKey(): String?

    /**
     * Stores the biometrics key.
     */
    fun storeUserBiometricUnlockKey(biometricsKey: String?)

    /**
     * Stores the symmetric key data used for encrypting TOTP data.
     */
    var authenticatorBridgeSymmetricSyncKey: ByteArray?
}
