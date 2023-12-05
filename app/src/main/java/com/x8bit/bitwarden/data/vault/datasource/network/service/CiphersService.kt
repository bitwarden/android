package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

/**
 * Provides an API for querying ciphers endpoints.
 */
interface CiphersService {
    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher>
}
