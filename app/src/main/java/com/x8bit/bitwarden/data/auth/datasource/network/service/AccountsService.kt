package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterResponseJson

/**
 * Provides an API for querying accounts endpoints.
 */
interface AccountsService {

    /**
     * Make delete account request.
     */
    suspend fun deleteAccount(masterPasswordHash: String): Result<Unit>

    /**
     * Make pre login request to get KDF params.
     */
    suspend fun preLogin(email: String): Result<PreLoginResponseJson>

    /**
     * Register a new account to Bitwarden.
     */
    suspend fun register(body: RegisterRequestJson): Result<RegisterResponseJson>

    /**
     * Request a password hint.
     */
    suspend fun requestPasswordHint(email: String): Result<PasswordHintResponseJson>
}
