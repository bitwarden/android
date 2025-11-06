package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.cxf.model.CredentialExchangeExportResponse
import com.bitwarden.cxf.model.CredentialExchangeProtocolMessage
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.service.CiphersService
import com.bitwarden.network.util.base64UrlDecodeOrNull
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.util.hasRestrictItemTypes
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val SUPPORTED_CXP_FORMAT_VERSIONS = mapOf(
    0 to setOf(0),
)
private val SUPPORTED_CXF_FORMAT_VERSIONS = mapOf(
    0 to setOf(0),
    1 to setOf(0),
)

/**
 * Default implementation of [CredentialExchangeImportManager].
 */
class CredentialExchangeImportManagerImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val ciphersService: CiphersService,
    private val vaultSyncManager: VaultSyncManager,
    private val policyManager: PolicyManager,
    private val json: Json,
) : CredentialExchangeImportManager {

    @Suppress("LongMethod")
    override suspend fun importCxfPayload(
        userId: String,
        payload: String,
    ): ImportCxfPayloadResult {
        val credentialExchangeExportResult = json
            .decodeFromStringOrNull<CredentialExchangeProtocolMessage>(payload)
            ?: return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException("Invalid CXP JSON."),
            )

        if (SUPPORTED_CXP_FORMAT_VERSIONS[credentialExchangeExportResult.version.major]
                ?.contains(credentialExchangeExportResult.version.minor) != true
        ) {
            return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException(
                    "Unsupported CXF version: ${credentialExchangeExportResult.version}.",
                ),
            )
        }

        val decodedPayload = credentialExchangeExportResult.payload
            .base64UrlDecodeOrNull()
            ?: return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException("Unable to decode payload."),
            )

        val exportResponse = json
            .decodeFromStringOrNull<CredentialExchangeExportResponse>(decodedPayload)
            ?: return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException("Unable to decode header."),
            )

        if (SUPPORTED_CXF_FORMAT_VERSIONS[exportResponse.version.major]
                ?.contains(exportResponse.version.minor) != true
        ) {
            return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException("Unsupported CXF version."),
            )
        }

        if (exportResponse.accounts.isEmpty()) {
            return ImportCxfPayloadResult.NoItems
        }

        val accountsJson = try {
            json.encodeToString(
                value = exportResponse.accounts.firstOrNull(),
            )
        } catch (_: SerializationException) {
            return ImportCxfPayloadResult.Error(
                ImportCredentialsInvalidJsonException("Unable to re-encode accounts."),
            )
        }
        return vaultSdkSource
            .importCxf(
                userId = userId,
                payload = accountsJson,
            )
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
                ciphersService
                    .importCiphers(
                        request = ImportCiphersJsonRequest(
                            ciphers = filteredCipherList.map {
                                it.toEncryptedNetworkCipher(
                                    encryptedFor = userId,
                                )
                            },
                            folders = emptyList(),
                            folderRelationships = emptyList(),
                        ),
                    )
                    .flatMap { importCiphersResponseJson ->
                        when (importCiphersResponseJson) {
                            is ImportCiphersResponseJson.Invalid -> {
                                ImportCredentialsUnknownErrorException().asFailure()
                            }

                            ImportCiphersResponseJson.Success -> {
                                ImportCxfPayloadResult
                                    .Success(itemCount = filteredCipherList.size)
                                    .asSuccess()
                            }
                        }
                    }
            }
            .map {
                when (val syncResult = vaultSyncManager.syncForResult(forced = true)) {
                    is SyncVaultDataResult.Success -> it
                    is SyncVaultDataResult.Error -> {
                        ImportCxfPayloadResult.SyncFailed(error = syncResult.throwable)
                    }
                }
            }
            .fold(
                onSuccess = { it },
                onFailure = { ImportCxfPayloadResult.Error(error = it) },
            )
    }
}
