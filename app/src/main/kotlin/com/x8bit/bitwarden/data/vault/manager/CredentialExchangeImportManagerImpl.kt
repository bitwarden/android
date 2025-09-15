package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher

/**
 * Default implementation of [CredentialExchangeImportManager].
 */
class CredentialExchangeImportManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val ciphersService: CiphersService,
) : CredentialExchangeImportManager {

    override suspend fun importCxfPayload(
        userId: String,
        payload: String,
    ): ImportCxfPayloadResult = vaultSdkSource
        .importCxf(
            userId = userId,
            payload = payload,
        )
        .flatMap { cipherList ->
            if (cipherList.isEmpty()) {
                // If no ciphers were returned, we can skip the remaining steps and return the
                // appropriate result.
                return ImportCxfPayloadResult.NoItems
            }
            ciphersService.importCiphers(
                request = ImportCiphersJsonRequest(
                    ciphers = cipherList.map {
                        it.toEncryptedNetworkCipher(
                            encryptedFor = userId,
                        )
                    },
                    folders = emptyList(),
                    folderRelationships = emptyList(),
                ),
            )
        }
        .flatMap { importCiphersResponseJson ->
            when (importCiphersResponseJson) {
                is ImportCiphersResponseJson.Invalid -> {
                    ImportCredentialsUnknownErrorException().asFailure()
                }

                ImportCiphersResponseJson.Success -> {
                    ImportCxfPayloadResult.Success
                        .asSuccess()
                }
            }
        }
        .fold(
            onSuccess = { it },
            onFailure = { ImportCxfPayloadResult.Error(error = it) },
        )
}
