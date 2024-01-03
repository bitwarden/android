package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson

/**
 * Provides an API for querying ciphers endpoints.
 */
interface CiphersService {
    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher>

    /**
     * Attempt to update a cipher.
     */
    suspend fun updateCipher(
        cipherId: String,
        body: CipherJsonRequest,
    ): Result<UpdateCipherResponseJson>
}
