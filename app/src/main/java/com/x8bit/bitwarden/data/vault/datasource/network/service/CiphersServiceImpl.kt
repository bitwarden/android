package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.vault.datasource.network.api.CiphersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

class CiphersServiceImpl constructor(
    private val ciphersApi: CiphersApi,
) : CiphersService {
    override suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher> =
        ciphersApi.createCipher(body = body)

    override suspend fun updateCipher(
        cipherId: String,
        body: CipherJsonRequest,
    ): Result<SyncResponseJson.Cipher> =
        ciphersApi.updateCipher(
            cipherId = cipherId,
            body = body,
        )
}
