package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson

/**
 * The default implementation of the [NewAuthRequestService].
 */
class NewAuthRequestServiceImpl(
    private val unauthenticatedAuthRequestsApi: UnauthenticatedAuthRequestsApi,
) : NewAuthRequestService {
    override suspend fun createAuthRequest(
        email: String,
        publicKey: String,
        deviceId: String,
        accessCode: String,
        fingerprint: String,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        unauthenticatedAuthRequestsApi.createAuthRequest(
            AuthRequestRequestJson(
                email = email,
                publicKey = publicKey,
                deviceId = deviceId,
                accessCode = accessCode,
                fingerprint = fingerprint,
                type = AuthRequestTypeJson.LOGIN_WITH_DEVICE,
            ),
        )

    override suspend fun getAuthRequestUpdate(
        requestId: String,
        accessCode: String,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        unauthenticatedAuthRequestsApi.getAuthRequestUpdate(
            requestId = requestId,
            accessCode = accessCode,
        )
}
