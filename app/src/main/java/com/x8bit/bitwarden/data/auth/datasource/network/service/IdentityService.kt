package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson

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
}
