package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.bitwarden.network.util.base64UrlEncode
import com.bitwarden.network.util.toResult
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedDevicesApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedDevicesApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TrustedDeviceKeysResponseJson

class DevicesServiceImpl(
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
