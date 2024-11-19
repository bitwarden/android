package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import com.x8bit.bitwarden.data.platform.util.asFailure

/**
 * The default implementation of the [NewAuthRequestService].
 */
class NewAuthRequestServiceImpl(
    private val authenticatedAuthRequestsApi: AuthenticatedAuthRequestsApi,
    private val unauthenticatedAuthRequestsApi: UnauthenticatedAuthRequestsApi,
) : NewAuthRequestService {
    override suspend fun createAuthRequest(
        email: String,
        publicKey: String,
        deviceId: String,
        accessCode: String,
        fingerprint: String,
        authRequestType: AuthRequestTypeJson,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        when (authRequestType) {
            AuthRequestTypeJson.LOGIN_WITH_DEVICE -> {
                unauthenticatedAuthRequestsApi
                    .createAuthRequest(
                        deviceIdentifier = deviceId,
                        body = AuthRequestRequestJson(
                            email = email,
                            publicKey = publicKey,
                            deviceId = deviceId,
                            accessCode = accessCode,
                            fingerprint = fingerprint,
                            type = authRequestType,
                        ),
                    )
                    .toResult()
            }

            AuthRequestTypeJson.UNLOCK -> {
                UnsupportedOperationException("Unlock AuthRequestType is currently unsupported")
                    .asFailure()
            }

            AuthRequestTypeJson.ADMIN_APPROVAL -> {
                authenticatedAuthRequestsApi
                    .createAdminAuthRequest(
                        deviceIdentifier = deviceId,
                        body = AuthRequestRequestJson(
                            email = email,
                            publicKey = publicKey,
                            deviceId = deviceId,
                            accessCode = accessCode,
                            fingerprint = fingerprint,
                            type = authRequestType,
                        ),
                    )
                    .toResult()
            }
        }

    override suspend fun getAuthRequestUpdate(
        requestId: String,
        accessCode: String,
        isSso: Boolean,
    ): Result<AuthRequestsResponseJson.AuthRequest> =
        if (isSso) {
            authenticatedAuthRequestsApi
                .getAuthRequest(requestId = requestId)
                .toResult()
        } else {
            unauthenticatedAuthRequestsApi
                .getAuthRequestUpdate(
                    requestId = requestId,
                    accessCode = accessCode,
                )
                .toResult()
        }
}
