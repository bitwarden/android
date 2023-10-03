package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.GetTokenResponseJson

/**
 * Wraps raw retrofit identity API in a cleaner interface.
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
