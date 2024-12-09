package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.crypto.TrustDeviceResponse
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.network.service.DevicesService
import com.x8bit.bitwarden.data.auth.manager.util.toUserStateJson
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
        if (authDiskSource.getShouldTrustDevice(userId = userId) != true) {
            authDiskSource.storeIsTdeLoginComplete(userId = userId, isTdeLoginComplete = true)
            false.asSuccess()
        } else {
            vaultSdkSource
                .getTrustDevice(userId = userId)
                .flatMap { trustThisDevice(userId = userId, trustDeviceResponse = it) }
                .also {
                    authDiskSource.storeShouldTrustDevice(
                        userId = userId,
                        shouldTrustDevice = null,
                    )
                }
                .map { true }
        }

    override suspend fun trustThisDevice(
        userId: String,
        trustDeviceResponse: TrustDeviceResponse,
    ): Result<Unit> = devicesService
        .trustDevice(
            appId = authDiskSource.uniqueAppId,
            encryptedDevicePrivateKey = trustDeviceResponse.protectedDevicePrivateKey,
            encryptedDevicePublicKey = trustDeviceResponse.protectedDevicePublicKey,
            encryptedUserKey = trustDeviceResponse.protectedUserKey,
        )
        .onSuccess {
            authDiskSource.storeDeviceKey(
                userId = userId,
                deviceKey = trustDeviceResponse.deviceKey,
            )
            authDiskSource.userState = trustDeviceResponse.toUserStateJson(
                userId = userId,
                previousUserState = requireNotNull(authDiskSource.userState),
            )
            authDiskSource.storeIsTdeLoginComplete(userId = userId, isTdeLoginComplete = true)
        }
        .also { authDiskSource.storeShouldTrustDevice(userId = userId, shouldTrustDevice = null) }
        .map { }
}
