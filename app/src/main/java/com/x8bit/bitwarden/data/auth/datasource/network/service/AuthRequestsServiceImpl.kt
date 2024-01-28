package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestUpdateRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson

class AuthRequestsServiceImpl(
    private val authRequestsApi: AuthRequestsApi,
) : AuthRequestsService {
    override suspend fun getAuthRequests(): Result<AuthRequestsResponseJson> =
        authRequestsApi.getAuthRequests()

    override suspend fun updateAuthRequest(
        requestId: String,
        key: String,
        masterPasswordHash: String?,
        deviceId: String,
        isApproved: Boolean,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        authRequestsApi.updateAuthRequest(
            userId = requestId,
            body = AuthRequestUpdateRequestJson(
                key = key,
                masterPasswordHash = masterPasswordHash,
                deviceId = deviceId,
                isApproved = isApproved,
            ),
        )
}
