package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.IdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import com.x8bit.bitwarden.data.platform.datasource.network.util.executeForResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
import kotlinx.serialization.json.Json
import java.util.UUID

class IdentityServiceImpl constructor(
    private val api: IdentityApi,
    private val json: Json,
    private val deviceModelProvider: DeviceModelProvider = DeviceModelProvider(),
) : IdentityService {

    @Suppress("MagicNumber")
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
                code = 400,
                json = json,
            ) ?: bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.Invalid>(
                code = 400,
                json = json,
            ) ?: throw throwable
        }

    override fun refreshTokenSynchronously(
        refreshToken: String,
    ): Result<RefreshTokenResponseJson> = api
        .refreshTokenCall(
            clientId = "mobile",
            grantType = "refresh_token",
            refreshToken = refreshToken,
        )
        .executeForResult()
}
