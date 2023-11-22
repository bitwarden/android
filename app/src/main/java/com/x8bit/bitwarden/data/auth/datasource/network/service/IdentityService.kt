package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RefreshTokenResponseJson

/**
 * Provides an API for querying identity endpoints.
 */
interface IdentityService {

    /**
     * Make request to get an access token.
     *
     * @param email user's email address.
     * @param passwordHash password hashed with the Bitwarden SDK.
     * @param captchaToken captcha token to be passed to the API (nullable).
     */
    suspend fun getToken(
        email: String,
        passwordHash: String,
        captchaToken: String?,
    ): Result<GetTokenResponseJson>

    /**
     * Synchronously makes a request to get refresh the access token.
     *
     * @param refreshToken The refresh token needed to obtain a new token.
     */
    fun refreshTokenSynchronously(refreshToken: String): Result<RefreshTokenResponseJson>
}
