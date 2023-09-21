package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson

/**
 * Wraps raw retrofit accounts API in a cleaner interface.
 */
interface AccountsService {

    /**
     * Make pre login request to get KDF params.
     */
    suspend fun preLogin(email: String): Result<PreLoginResponseJson>
}
