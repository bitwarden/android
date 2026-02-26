package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
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
import timber.log.Timber

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
                    accountsJsonList = exportResponse.accountsJsonList,
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
        accountsJsonList: List<String>,
    ): ImportCxfPayloadResult {
        val allCiphers = accountsJsonList.flatMap { accountJson ->
            vaultSdkSource
                .importCxf(userId = userId, payload = accountJson)
                .getOrElse { return ImportCxfPayloadResult.Error(error = it) }
        }

        // Filter out card ciphers if RESTRICT_ITEM_TYPES policy is active
        val filteredCipherList = if (policyManager.hasRestrictItemTypes()) {
            allCiphers.filter { cipher -> cipher.type != CipherType.CARD }
        } else {
            allCiphers
        }

        if (filteredCipherList.isEmpty()) {
            return ImportCxfPayloadResult.NoItems
        }

        return uploadCiphers(userId = userId, ciphers = filteredCipherList)
            .map { syncVault(it) }
            .fold(
                onSuccess = { it },
                onFailure = { ImportCxfPayloadResult.Error(error = it) },
            )
    }

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
                        Timber.w(
                            "Import ciphers validation failed: %s",
                            response.validationErrors,
                        )
                        ImportCredentialsUnknownErrorException().asFailure()
                    }

                    is ImportCiphersResponseJson.Success -> {
                        ImportCxfPayloadResult.Success(itemCount = ciphers.size).asSuccess()
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
