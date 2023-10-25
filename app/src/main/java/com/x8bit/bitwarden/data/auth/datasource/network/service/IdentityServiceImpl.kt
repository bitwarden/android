package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.util.UUID

class IdentityServiceImpl constructor(
    private val api: IdentityApi,
    private val json: Json,
    private val deviceModelProvider: DeviceModelProvider = DeviceModelProvider(),
) : IdentityService {

    override suspend fun getToken(
        email: String,
        passwordHash: String,
        captchaToken: String?,
    ): Result<GetTokenResponseJson> = api
        .getToken(
            scope = "api+offline_access",
            clientId = "mobile",
            authEmail = email.base64UrlEncode(),
            // TODO: use correct device identifier here BIT-325
            deviceIdentifier = UUID.randomUUID().toString(),
            deviceName = deviceModelProvider.deviceModel,
            deviceType = "0",
            grantType = "password",
            passwordHash = passwordHash,
            email = email,
            captchaResponse = captchaToken,
        )
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.CaptchaRequired>(
                code = HTTP_BAD_REQUEST,
                json = json,
            ) ?: bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.Invalid>(
                code = HTTP_BAD_REQUEST,
                json = json,
            ) ?: throw throwable
        }
}
