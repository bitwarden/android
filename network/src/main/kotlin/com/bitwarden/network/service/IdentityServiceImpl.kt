package com.bitwarden.network.service

import com.bitwarden.network.api.UnauthenticatedIdentityApi
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.IdentityTokenAuthModel
import com.bitwarden.network.model.PreLoginRequestJson
import com.bitwarden.network.model.PreLoginResponseJson
import com.bitwarden.network.model.PrevalidateSsoResponseJson
import com.bitwarden.network.model.RefreshTokenResponseJson
import com.bitwarden.network.model.RegisterFinishRequestJson
import com.bitwarden.network.model.RegisterRequestJson
import com.bitwarden.network.model.RegisterResponseJson
import com.bitwarden.network.model.SendVerificationEmailRequestJson
import com.bitwarden.network.model.SendVerificationEmailResponseJson
import com.bitwarden.network.model.TwoFactorDataModel
import com.bitwarden.network.model.VerifyEmailTokenRequestJson
import com.bitwarden.network.model.VerifyEmailTokenResponseJson
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.util.DeviceModelProvider
import com.bitwarden.network.util.NetworkErrorCode
import com.bitwarden.network.util.executeForNetworkResult
import com.bitwarden.network.util.getNetworkErrorCodeOrNull
import com.bitwarden.network.util.parseErrorBodyOrNull
import com.bitwarden.network.util.toResult
import kotlinx.serialization.json.Json

internal class IdentityServiceImpl(
    private val unauthenticatedIdentityApi: UnauthenticatedIdentityApi,
    private val json: Json,
    private val deviceModelProvider: DeviceModelProvider = DeviceModelProvider(),
) : IdentityService {

    override suspend fun preLogin(email: String): Result<PreLoginResponseJson> =
        unauthenticatedIdentityApi
            .preLogin(PreLoginRequestJson(email = email))
            .toResult()

    override suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson> =
        unauthenticatedIdentityApi
            .register(body)
            .toResult()
            .recoverCatching { throwable ->
                val bitwardenError = throwable.toBitwardenError()
                bitwardenError
                    .parseErrorBodyOrNull<RegisterResponseJson.Invalid>(
                        codes = listOf(
                            NetworkErrorCode.BAD_REQUEST,
                            NetworkErrorCode.TOO_MANY_REQUESTS,
                        ),
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun getToken(
        uniqueAppId: String,
        email: String,
        authModel: IdentityTokenAuthModel,
        twoFactorData: TwoFactorDataModel?,
        newDeviceOtp: String?,
    ): Result<GetTokenResponseJson> = unauthenticatedIdentityApi
        .getToken(
            scope = "api offline_access",
            clientId = "mobile",
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
            authRequestId = authModel.authRequestId,
            newDeviceOtp = newDeviceOtp,
        )
        .toResult()
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError
                .parseErrorBodyOrNull<GetTokenResponseJson.TwoFactorRequired>(
                    code = NetworkErrorCode.BAD_REQUEST,
                    json = json,
                )
                ?: bitwardenError.parseErrorBodyOrNull<GetTokenResponseJson.Invalid>(
                    code = NetworkErrorCode.BAD_REQUEST,
                    json = json,
                )
                ?: throw throwable
        }

    override suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): Result<PrevalidateSsoResponseJson> = unauthenticatedIdentityApi
        .prevalidateSso(
            organizationIdentifier = organizationIdentifier,
        )
        .toResult()
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError
                .parseErrorBodyOrNull<PrevalidateSsoResponseJson.Error>(
                    code = NetworkErrorCode.BAD_REQUEST,
                    json = json,
                )
                ?: throw throwable
        }

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
        .recoverCatching { throwable ->
            val bitwardenError = throwable.toBitwardenError()
            bitwardenError
                .parseErrorBodyOrNull<RefreshTokenResponseJson.Error>(
                    code = NetworkErrorCode.BAD_REQUEST,
                    json = json,
                )
                ?: run {
                    when (bitwardenError.getNetworkErrorCodeOrNull()) {
                        NetworkErrorCode.UNAUTHORIZED -> {
                            RefreshTokenResponseJson.Unauthorized(throwable)
                        }

                        NetworkErrorCode.FORBIDDEN -> {
                            RefreshTokenResponseJson.Forbidden(throwable)
                        }

                        NetworkErrorCode.BAD_REQUEST,
                        NetworkErrorCode.TOO_MANY_REQUESTS,
                        null,
                            -> throw throwable
                    }
                }
        }

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
                        codes = listOf(
                            NetworkErrorCode.BAD_REQUEST,
                            NetworkErrorCode.TOO_MANY_REQUESTS,
                        ),
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
                        code = NetworkErrorCode.BAD_REQUEST,
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
                    code = NetworkErrorCode.BAD_REQUEST,
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
