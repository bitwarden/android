package com.x8bit.bitwarden.data.platform.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest

/**
 * Provides an API for push tokens.
 */
interface PushService {
    /**
     * Updates the user's push token.
     */
    suspend fun putDeviceToken(
        body: PushTokenRequest,
    ): Result<Unit>
}
