package com.x8bit.bitwarden.data.vault.datasource.network.api

import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Defines raw calls under the /ciphers API with authentication applied.
 */
interface CiphersApi {

    /**
     * Create a cipher.
     */
    @POST("ciphers")
    suspend fun createCipher(@Body body: CipherJsonRequest): Result<SyncResponseJson.Cipher>
}
