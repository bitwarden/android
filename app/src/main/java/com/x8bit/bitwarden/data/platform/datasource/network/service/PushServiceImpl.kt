package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.api.PushApi
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest

class PushServiceImpl(
    private val pushApi: PushApi,
    private val appId: String,
) : PushService {
    override suspend fun putDeviceToken(
        body: PushTokenRequest,
    ): Result<Unit> =
        pushApi
            .putDeviceToken(
                appId = appId,
                body = body,
            )
}
