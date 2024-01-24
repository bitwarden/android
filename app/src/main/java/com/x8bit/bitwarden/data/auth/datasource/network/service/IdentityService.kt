package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.IdentityTokenAuthModel
import com.x8bit.bitwarden.data.auth.datasource.network.model.PrevalidateSsoResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorDataModel

/**
 * Provides an API for querying identity endpoints.
 */
interface IdentityService {

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
}
