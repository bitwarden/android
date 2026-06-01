package com.bitwarden.network.service

import com.bitwarden.network.api.AuthenticatedAuthRequestsApi
import com.bitwarden.network.model.AuthRequestUpdateRequestJson
import com.bitwarden.network.model.AuthRequestsResponseJson
import com.bitwarden.network.util.toResult

internal class AuthRequestsServiceImpl(
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
