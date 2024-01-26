package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthRequestsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.AuthRequestsResponseJson

class AuthRequestsServiceImpl(
    private val authRequestsApi: AuthRequestsApi,
) : AuthRequestsService {
    override suspend fun getAuthRequests(): Result<AuthRequestsResponseJson> =
        authRequestsApi.getAuthRequests()
}
