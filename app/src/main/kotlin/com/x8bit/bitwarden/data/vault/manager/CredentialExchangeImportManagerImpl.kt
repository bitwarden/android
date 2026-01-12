package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.cxf.model.CredentialExchangePayload
import com.bitwarden.cxf.parser.CredentialExchangePayloadParser
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.service.CiphersService
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.util.hasRestrictItemTypes
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher

/**
 * Default implementation of [CredentialExchangeImportManager].
 */
class CredentialExchangeImportManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val ciphersService: CiphersService,
    private val vaultSyncManager: VaultSyncManager,
    private val policyManager: PolicyManager,
    private val credentialExchangePayloadParser: CredentialExchangePayloadParser,
) : CredentialExchangeImportManager {

    override suspend fun importCxfPayload(
        userId: String,
        payload: String,
    ): ImportCxfPayloadResult =
        when (val exportResponse = credentialExchangePayloadParser.parse(payload)) {
            is CredentialExchangePayload.Importable -> {
                import(
                    userId = userId,
                    accountsJson = exportResponse.accountsJson,
                )
            }

            CredentialExchangePayload.NoItems -> {
                ImportCxfPayloadResult.NoItems
            }

            is CredentialExchangePayload.Error -> {
                ImportCxfPayloadResult.Error(exportResponse.throwable)
            }
        }

    private suspend fun import(
        userId: String,
        accountsJson: String,
    ): ImportCxfPayloadResult = vaultSdkSource
        .importCxf(userId = userId, payload = accountsJson)
        .flatMap { cipherList ->
            // Filter out card ciphers if RESTRICT_ITEM_TYPES policy is active
            val filteredCipherList = if (policyManager.hasRestrictItemTypes()) {
                cipherList.filter { cipher -> cipher.type != CipherType.CARD }
            } else {
                cipherList
            }

            if (filteredCipherList.isEmpty()) {
                // If no ciphers were returned, we can skip the remaining steps and return the
                // appropriate result.
                return ImportCxfPayloadResult.NoItems
            }
            uploadCiphers(userId = userId, ciphers = filteredCipherList)
        }
        .map { syncVault(it) }
        .fold(
            onSuccess = { it },
            onFailure = { ImportCxfPayloadResult.Error(error = it) },
        )

    private suspend fun uploadCiphers(
        userId: String,
        ciphers: List<Cipher>,
    ): Result<ImportCxfPayloadResult.Success> {
        val request = ImportCiphersJsonRequest(
            ciphers = ciphers.map { it.toEncryptedNetworkCipher(encryptedFor = userId) },
            folders = emptyList(),
            folderRelationships = emptyList(),
        )
        return ciphersService
            .importCiphers(request)
            .flatMap { response ->
                when (response) {
                    is ImportCiphersResponseJson.Invalid -> {
                        Result.failure(ImportCredentialsUnknownErrorException())
                    }

                    is ImportCiphersResponseJson.Success -> {
                        Result.success(
                            ImportCxfPayloadResult.Success(itemCount = ciphers.size),
                        )
                    }
                }
            }
    }

    private suspend fun syncVault(result: ImportCxfPayloadResult): ImportCxfPayloadResult =
        when (val syncResult = vaultSyncManager.syncForResult(forced = true)) {
            is SyncVaultDataResult.Success -> result
            is SyncVaultDataResult.Error -> {
                ImportCxfPayloadResult.SyncFailed(error = syncResult.throwable)
            }
        }
}
