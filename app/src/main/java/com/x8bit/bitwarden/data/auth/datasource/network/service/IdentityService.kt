package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.IdentityTokenAuthModel
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

/**
 * Provides an API for querying identity endpoints.
 */
interface IdentityService {

    /**
     * Make pre login request to get KDF params.
     */
    suspend fun preLogin(email: String): Result<PreLoginResponseJson>

    /**
     * Register a new account to Bitwarden.
     */
    suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson>

    /**
     * Make request to get an access token.
     *
     * @param uniqueAppId applications unique identifier.
     * @param email user's email address.
     * @param authModel information necessary to authenticate with any
     * of the available login methods.
     * @param captchaToken captcha token to be passed to the API (nullable).
     * @param twoFactorData the two-factor data, if applicable.
     */
    @Suppress("LongParameterList")
    suspend fun getToken(
        uniqueAppId: String,
        email: String,
        authModel: IdentityTokenAuthModel,
        captchaToken: String?,
        twoFactorData: TwoFactorDataModel? = null,
    ): Result<GetTokenResponseJson>

    /**
     * Prevalidates the organization identifier used in an SSO request.
     *
     * @param organizationIdentifier The SSO organization identifier.
     */
    suspend fun prevalidateSso(
        organizationIdentifier: String,
    ): Result<PrevalidateSsoResponseJson>

    /**
     * Synchronously makes a request to get refresh the access token.
     *
     * @param refreshToken The refresh token needed to obtain a new token.
     */
    fun refreshTokenSynchronously(refreshToken: String): Result<RefreshTokenResponseJson>

    /**
     * Send a verification email.
     */
    suspend fun sendVerificationEmail(
        body: SendVerificationEmailRequestJson,
    ): Result<SendVerificationEmailResponseJson>

    /**
     * Register a new account to Bitwarden using email verification flow.
     */
    suspend fun registerFinish(body: RegisterFinishRequestJson): Result<RegisterResponseJson>

    /**
     * Makes request to verify email registration token. If the token provided is
     * still valid will return success.
     */
    suspend fun verifyEmailRegistrationToken(
        body: VerifyEmailTokenRequestJson,
    ): Result<VerifyEmailTokenResponseJson>
}
