package com.bitwarden.network.service

import com.bitwarden.network.api.AuthenticatedDevicesApi
import com.bitwarden.network.api.UnauthenticatedDevicesApi
import com.bitwarden.network.model.TrustedDeviceKeysRequestJson
import com.bitwarden.network.model.TrustedDeviceKeysResponseJson
import com.bitwarden.network.util.base64UrlEncode
import com.bitwarden.network.util.toResult

/**
 * The default implementation of the [DevicesService].
 */
internal class DevicesServiceImpl(
    private val authenticatedDevicesApi: AuthenticatedDevicesApi,
    private val unauthenticatedDevicesApi: UnauthenticatedDevicesApi,
) : DevicesService {
    override suspend fun getIsKnownDevice(
        emailAddress: String,
        deviceId: String,
    ): Result<Boolean> = unauthenticatedDevicesApi
        .getIsKnownDevice(
            emailAddress = emailAddress.base64UrlEncode(),
            deviceId = deviceId,
        )
        .toResult()

    override suspend fun trustDevice(
        appId: String,
        encryptedUserKey: String,
        encryptedDevicePublicKey: String,
        encryptedDevicePrivateKey: String,
    ): Result<TrustedDeviceKeysResponseJson> = authenticatedDevicesApi
        .updateTrustedDeviceKeys(
            appId = appId,
            request = TrustedDeviceKeysRequestJson(
                encryptedUserKey = encryptedUserKey,
                encryptedDevicePublicKey = encryptedDevicePublicKey,
                encryptedDevicePrivateKey = encryptedDevicePrivateKey,
            ),
        )
        .toResult()
}
