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
import com.x8bit.bitwarden.data.auth.datasource.network.model.SendVerificationEmailResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyEmailTokenRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.VerifyEmailTokenResponseJson
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.base64UrlEncode
import com.x8bit.bitwarden.data.platform.datasource.network.util.executeForNetworkResult
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import com.x8bit.bitwarden.data.platform.util.DeviceModelProvider
import kotlinx.serialization.json.Json

class IdentityServiceImpl(
    private val unauthenticatedIdentityApi: UnauthenticatedIdentityApi,
    private val json: Json,
    private val deviceModelProvider: DeviceModelProvider = DeviceModelProvider(),
) : IdentityService {

    override suspend fun preLogin(email: String): Result<PreLoginResponseJson> =
        unauthenticatedIdentityApi
            .preLogin(PreLoginRequestJson(email = email))
            .toResult()

    @Suppress("MagicNumber")
    override suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson> =
        unauthenticatedIdentityApi
            .register(body)
            .toResult()
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
        .toResult()
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
        .toResult()

    override fun refreshTokenSynchronously(
        refreshToken: String,
    ): Result<RefreshTokenResponseJson> = unauthenticatedIdentityApi
        .refreshTokenCall(
            clientId = "mobile",
            grantType = "refresh_token",
            refreshToken = refreshToken,
        )
        .executeForNetworkResult()
        .toResult()

    @Suppress("MagicNumber")
    override suspend fun registerFinish(
        body: RegisterFinishRequestJson,
    ): Result<RegisterResponseJson> =
        unauthenticatedIdentityApi
            .registerFinish(body)
            .toResult()
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
    ): Result<SendVerificationEmailResponseJson> {
        return unauthenticatedIdentityApi
            .sendVerificationEmail(body = body)
            .toResult()
            .map { SendVerificationEmailResponseJson.Success(it?.content) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<SendVerificationEmailResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }
    }

    override suspend fun verifyEmailRegistrationToken(
        body: VerifyEmailTokenRequestJson,
    ): Result<VerifyEmailTokenResponseJson> = unauthenticatedIdentityApi
        .verifyEmailToken(
            body = body,
        )
        .toResult()
        .map { VerifyEmailTokenResponseJson.Valid }
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError
                .parseErrorBodyOrNull<VerifyEmailTokenResponseJson.Invalid>(
                    code = 400,
                    json = json,
                )
                ?.checkForExpiredMessage()
                ?: throw throwable
        }
}

/**
 * If the message body contains text related to the token being expired, return
 * the TokenExpired type. Otherwise, return the original Invalid response.
 */
private fun VerifyEmailTokenResponseJson.Invalid.checkForExpiredMessage() =
    if (message.contains(other = "expired", ignoreCase = true)) {
        VerifyEmailTokenResponseJson.TokenExpired
    } else {
        this
    }
