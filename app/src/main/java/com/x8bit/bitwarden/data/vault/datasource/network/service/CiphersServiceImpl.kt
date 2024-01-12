package com.x8bit.bitwarden.data.vault.datasource.network.service

import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.api.CiphersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import kotlinx.serialization.json.Json

class CiphersServiceImpl constructor(
    private val ciphersApi: CiphersApi,
    private val json: Json,
) : CiphersService {
    override suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher> =
        ciphersApi.createCipher(body = body)

    override suspend fun updateCipher(
        cipherId: String,
        body: CipherJsonRequest,
    ): Result<UpdateCipherResponseJson> =
        ciphersApi
            .updateCipher(
                cipherId = cipherId,
                body = body,
            )
            .map { UpdateCipherResponseJson.Success(cipher = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateCipherResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun deleteCipher(cipherId: String): Result<Unit> =
        ciphersApi.deleteCipher(cipherId = cipherId)
}
