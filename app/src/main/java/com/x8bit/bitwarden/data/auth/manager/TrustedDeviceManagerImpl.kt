package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource

/**
 * Default implementation of the [TrustedDeviceManager] used to establish trust with this device.
 */
class TrustedDeviceManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val devicesService: DevicesService,
) : TrustedDeviceManager {
    override suspend fun trustThisDeviceIfNecessary(userId: String): Result<Boolean> =
        if (!authDiskSource.shouldTrustDevice) {
            false.asSuccess()
        } else {
            vaultSdkSource
                .getTrustDevice(userId = userId)
                .flatMap { trustedDevice ->
                    devicesService
                        .trustDevice(
                            appId = authDiskSource.uniqueAppId,
                            encryptedDevicePrivateKey = trustedDevice.protectedDevicePrivateKey,
                            encryptedDevicePublicKey = trustedDevice.protectedDevicePublicKey,
                            encryptedUserKey = trustedDevice.protectedUserKey,
                        )
                        .onSuccess {
                            authDiskSource.storeDeviceKey(
                                userId = userId,
                                deviceKey = trustedDevice.deviceKey,
                            )
                        }
                }
                .also { authDiskSource.shouldTrustDevice = false }
                .map { true }
        }
}
