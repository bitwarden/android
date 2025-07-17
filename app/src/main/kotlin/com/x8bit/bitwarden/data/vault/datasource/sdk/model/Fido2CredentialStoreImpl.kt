package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.EncryptionContext
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult
import timber.log.Timber

/**
 * Primary implementation of [Fido2CredentialStore].
 */
@OmitFromCoverage
class Fido2CredentialStoreImpl(
    private val vaultRepository: VaultRepository,
) : Fido2CredentialStore {

    /**
     * Return all active ciphers that contain FIDO 2 credentials.
     */
    override suspend fun allCredentials(): List<CipherListView> {
        val syncResult = vaultRepository.syncForResult()
        if (syncResult is SyncVaultDataResult.Error) {
            syncResult.throwable
                ?.let { throw it }
                ?: throw IllegalStateException("Sync failed.")
        }
        return vaultRepository
            .decryptCipherListResultStateFlow
            .value
            .data
            ?.successes
            .orEmpty()
            .filter { it.login?.hasFido2 ?: false }
    }

    /**
     * Returns ciphers that contain FIDO 2 credentials for the given [ripId] with the provided
     * [ids].
     *
     * @param ids Optional list of FIDO 2 credential ID's to find.
     * @param ripId Relying Party ID to find.
     */
    override suspend fun findCredentials(ids: List<ByteArray>?, ripId: String): List<CipherView> {
        val syncResult = vaultRepository.syncForResult()
        if (syncResult is SyncVaultDataResult.Error) {
            syncResult.throwable
                ?.let { throw it }
                ?: throw IllegalStateException("Sync failed.")
        }

        return vaultRepository
            .decryptCipherListResultStateFlow
            .value
            .data
            ?.successes
            .orEmpty()
            .filterMatchingCredentials(
                credentialIds = ids,
                relyingPartyId = ripId,
            )
            .mapNotNull { cipherListView ->
                cipherListView.id
                    ?.let { cipherId ->
                        when (val result = vaultRepository.getCipher(cipherId = cipherId)) {
                            GetCipherResult.CipherNotFound -> {
                                Timber.e("Cipher not found.")
                                null
                            }

                            is GetCipherResult.Failure -> {
                                Timber.e(result.error, "Failed to decrypt cipher.")
                                null
                            }

                            is GetCipherResult.Success -> result.cipherView
                        }
                    }
            }
    }

    /**
     * Save the provided [cred] to the users vault.
     */
    override suspend fun saveCredential(cred: EncryptionContext) {
        when (val result = vaultRepository.getCipher(cred.cipher.id.orEmpty())) {
            GetCipherResult.CipherNotFound -> Unit
            is GetCipherResult.Failure -> result.error?.let { throw it }
            is GetCipherResult.Success -> {
                result.cipherView.id
                    ?.let { id ->
                        vaultRepository.updateCipher(
                            cipherId = id,
                            cipherView = result.cipherView,
                        )
                    }
                    ?: vaultRepository.createCipher(cipherView = result.cipherView)
            }
        }
    }

    /**
     * Return a filtered list containing elements that match the given [relyingPartyId] and a
     * credential ID contained in [credentialIds].
     */
    private fun List<CipherListView>.filterMatchingCredentials(
        credentialIds: List<ByteArray>?,
        relyingPartyId: String,
    ): List<CipherListView> {
        val skipCredentialIdFiltering = credentialIds.isNullOrEmpty()
        return filter { cipherListView ->
            val hasMatchingRpId = cipherListView.login
                ?.fido2Credentials
                .orEmpty()
                .any { it.rpId == relyingPartyId }

            val fido2CredentialIds = cipherListView.login
                ?.fido2Credentials
                .orEmpty()
                .map { it.credentialId.toByteArray() }

            hasMatchingRpId &&
                (
                    skipCredentialIdFiltering ||
                        credentialIds
                            .intersect(fido2CredentialIds)
                            .isNotEmpty()
                    )
        }
    }
}
