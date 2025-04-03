package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.bitwarden.network.api.PushApi
import com.bitwarden.network.model.PushTokenRequest
import com.bitwarden.network.util.toResult

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
            .toResult()
}
