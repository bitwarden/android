package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestUpdateRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult

class AuthRequestsServiceImpl(
    private val authenticatedAuthRequestsApi: AuthenticatedAuthRequestsApi,
) : AuthRequestsService {
    override suspend fun getAuthRequests(): Result<AuthRequestsResponseJson> =
        authenticatedAuthRequestsApi
            .getAuthRequests()
            .toResult()

    override suspend fun getAuthRequest(
        requestId: String,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        authenticatedAuthRequestsApi
            .getAuthRequest(requestId = requestId)
            .toResult()

    override suspend fun updateAuthRequest(
        requestId: String,
        key: String,
        masterPasswordHash: String?,
        deviceId: String,
        isApproved: Boolean,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        authenticatedAuthRequestsApi
            .updateAuthRequest(
                userId = requestId,
                body = AuthRequestUpdateRequestJson(
                    key = key,
                    masterPasswordHash = masterPasswordHash,
                    deviceId = deviceId,
                    isApproved = isApproved,
                ),
            )
            .toResult()
}
