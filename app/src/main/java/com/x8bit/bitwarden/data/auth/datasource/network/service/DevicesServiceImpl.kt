package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.DevicesApi
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode

class DevicesServiceImpl(
    private val devicesApi: DevicesApi,
) : DevicesService {
    override suspend fun getIsKnownDevice(
        emailAddress: String,
        deviceId: String,
    ): Result<Boolean> = devicesApi.getIsKnownDevice(
        emailAddress = emailAddress.base64UrlEncode(),
        deviceId = deviceId,
    )
}
