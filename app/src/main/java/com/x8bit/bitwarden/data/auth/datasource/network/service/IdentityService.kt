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
     */
    suspend fun getToken(
        email: String,
        passwordHash: String,
    ): Result<GetTokenResponseJson>
}
