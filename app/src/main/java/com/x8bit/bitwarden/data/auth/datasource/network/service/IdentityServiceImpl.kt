package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedIdentityApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.IdentityTokenAuthModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PrevalidateSsoResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterFinishRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SendVerificationEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import com.x8bit.bitwarden.data.platform.datasource.network.util.executeForResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
import kotlinx.serialization.json.Json

class IdentityServiceImpl(
    private val unauthenticatedIdentityApi: UnauthenticatedIdentityApi,
    private val json: Json,
    private val deviceModelProvider: DeviceModelProvider = DeviceModelProvider(),
) : IdentityService {

    override suspend fun preLogin(email: String): Result<PreLoginResponseJson> =
        unauthenticatedIdentityApi.preLogin(PreLoginRequestJson(email = email))

    @Suppress("MagicNumber")
    override suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson> =
        unauthenticatedIdentityApi
            .register(body)
            .recoverCatching { throwable ->
                val bitwardenError = throwable.toBitwardenError()
                bitwardenError
                    .parseErrorBodyOrNull<RegisterResponseJson.CaptchaRequired>(
                        code = 400,
                        json = json,
                    )
                    ?: bitwardenError.parseErrorBodyOrNull<RegisterResponseJson.Invalid>(
                        codes = listOf(400, 429),
                        json = json,
                    )
                    ?: throw throwable
            }

    @Suppress("MagicNumber")
    override suspend fun getToken(
        uniqueAppId: String,
        email: String,
        authModel: IdentityTokenAuthModel,
        captchaToken: String?,
        twoFactorData: TwoFactorDataModel?,
    ): Result<GetTokenResponseJson> = unauthenticatedIdentityApi
        .getToken(
            scope = "api offline_access",
            clientId = "mobile",
            authEmail = email.base64UrlEncode(),
            deviceIdentifier = uniqueAppId,
            deviceName = deviceModelProvider.deviceModel,
            deviceType = "0",
            grantType = authModel.grantType,
            passwordHash = authModel.password,
            email = email,
            ssoCode = authModel.ssoCode,
            ssoCodeVerifier = authModel.ssoCodeVerifier,
            ssoRedirectUri = authModel.ssoRedirectUri,
            twoFactorCode = twoFactorData?.code,
            twoFactorMethod = twoFactorData?.method,
            twoFactorRemember = twoFactorData?.remember?.let { if (it) "1" else "0 " },
            captchaResponse = captchaToken,
            authRequestId = authModel.authRequestId,
        )
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.CaptchaRequired>(
                code = 400,
                json = json,
            ) ?: bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.TwoFactorRequired>(
                code = 400,
                json = json,
            ) ?: bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.Invalid>(
                code = 400,
                json = json,
            ) ?: throw throwable
        }

    override suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): Result<PrevalidateSsoResponseJson> = unauthenticatedIdentityApi
        .prevalidateSso(
            organizationIdentifier = organizationIdentifier,
        )

    override fun refreshTokenSynchronously(
        refreshToken: String,
    ): Result<RefreshTokenResponseJson> = unauthenticatedIdentityApi
        .refreshTokenCall(
            clientId = "mobile",
            grantType = "refresh_token",
            refreshToken = refreshToken,
        )
        .executeForResult()

    @Suppress("MagicNumber")
    override suspend fun registerFinish(
        body: RegisterFinishRequestJson,
    ): Result<RegisterResponseJson> =
        unauthenticatedIdentityApi
            .registerFinish(body)
            .recoverCatching { throwable ->
                val bitwardenError = throwable.toBitwardenError()
                bitwardenError
                    .parseErrorBodyOrNull<RegisterResponseJson.Invalid>(
                        codes = listOf(400, 429),
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun sendVerificationEmail(
        body: SendVerificationEmailRequestJson,
    ): Result<String?> {
        return unauthenticatedIdentityApi
            .sendVerificationEmail(body = body)
            .map { it?.content }
    }
}
