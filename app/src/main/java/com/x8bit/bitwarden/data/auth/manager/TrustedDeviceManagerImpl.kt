package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.manager.util.toUserStateJson
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
            // Even though we are not trusting the device, we still store the device key in
            // memory. This allows the user to be "trusted" for this session but on timeout
            // or reboot, the "trust" will be gone.
            vaultSdkSource
                .getTrustDevice(userId = userId)
                .onSuccess { trustedDevice ->
                    authDiskSource.storeDeviceKey(
                        userId = userId,
                        deviceKey = trustedDevice.deviceKey,
                        inMemoryOnly = true,
                    )
                }
                .map { false }
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
                            authDiskSource.userState = trustedDevice.toUserStateJson(
                                userId = userId,
                                previousUserState = requireNotNull(authDiskSource.userState),
                            )
                        }
                }
                .also { authDiskSource.shouldTrustDevice = false }
                .map { true }
        }
}
