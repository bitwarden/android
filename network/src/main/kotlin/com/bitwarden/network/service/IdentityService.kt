package com.bitwarden.network.service

import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.IdentityTokenAuthModel
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
     * @param twoFactorData the two-factor data, if applicable.
     */
    @Suppress("LongParameterList")
    suspend fun getToken(
        uniqueAppId: String,
        email: String,
        authModel: IdentityTokenAuthModel,
        twoFactorData: TwoFactorDataModel? = null,
        newDeviceOtp: String? = null,
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
