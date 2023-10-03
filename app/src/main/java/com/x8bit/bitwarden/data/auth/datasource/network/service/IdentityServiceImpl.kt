package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyAsResult
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.util.UUID

class IdentityServiceImpl constructor(
    private val api: IdentityApi,
    private val json: Json,
    // TODO: use correct base URL here BIT-328
    private val baseUrl: String = "https://vault.bitwarden.com",
) : IdentityService {

    override suspend fun getToken(
        email: String,
        passwordHash: String,
        captchaToken: String?,
    ): Result<GetTokenResponseJson> = api
        .getToken(
            // TODO: use correct base URL here BIT-328
            url = "$baseUrl/identity/connect/token",
            scope = "api+offline_access",
            clientId = "mobile",
            authEmail = email.base64UrlEncode(),
            // TODO: use correct device identifier here BIT-325
            deviceIdentifier = UUID.randomUUID().toString(),
            // TODO: use correct values for deviceName and deviceType BIT-326
            deviceName = "Pixel 6",
            deviceType = "0",
            grantType = "password",
            passwordHash = passwordHash,
            email = email,
            captchaResponse = captchaToken,
        )
        .fold(
            onSuccess = { Result.success(it) },
            onFailure = {
                it.parseErrorBodyAsResult<GetTokenResponseJson.CaptchaRequired>(
                    code = HTTP_BAD_REQUEST,
                    json = json,
                )
            },
        )
}
