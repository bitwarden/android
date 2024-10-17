package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SyncVaultDataResult

/**
 * Primary implementation of [Fido2CredentialStore].
 */
@OmitFromCoverage
class Fido2CredentialStoreImpl(
    private val vaultSdkSource: VaultSdkSource,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : Fido2CredentialStore {

    /**
     * Return all active ciphers that contain FIDO 2 credentials.
     */
    override suspend fun allCredentials(): List<CipherView> {
        val syncResult = vaultRepository.syncForResult()
        if (syncResult is SyncVaultDataResult.Error) {
            syncResult.throwable
                ?.let { throw it }
                ?: throw IllegalStateException("Sync failed.")
        }
        return vaultRepository.ciphersStateFlow.value.data
            ?.filter { it.isActiveWithFido2Credentials }
            ?: emptyList()
    }

    /**
     * Returns ciphers that contain FIDO 2 credentials for the given [ripId] with the provided
     * [ids].
     *
     * @param ids Optional list of FIDO 2 credential ID's to find.
     * @param ripId Relying Party ID to find.
     */
    override suspend fun findCredentials(ids: List<ByteArray>?, ripId: String): List<CipherView> {
        val userId = getActiveUserIdOrThrow()

        val syncResult = vaultRepository.syncForResult()
        if (syncResult is SyncVaultDataResult.Error) {
            syncResult.throwable
                ?.let { throw it }
                ?: throw IllegalStateException("Sync failed.")
        }

        val ciphersWithFido2Credentials = vaultRepository.ciphersStateFlow.value.data
            ?.filter { it.isActiveWithFido2Credentials }
            .orEmpty()

        return vaultSdkSource
            .decryptFido2CredentialAutofillViews(
                userId = userId,
                cipherViews = ciphersWithFido2Credentials.toTypedArray(),
            )
            .map { decryptedFido2CredentialViews ->
                decryptedFido2CredentialViews.filterMatchingCredentials(
                    ids,
                    ripId,
                )
            }
            .map { matchingFido2Credentials ->
                ciphersWithFido2Credentials.filter { cipherView ->
                    matchingFido2Credentials.any { it.cipherId == cipherView.id }
                }
            }
            .fold(
                onSuccess = { it },
                onFailure = { throw it },
            )
    }

    /**
     * Save the provided [cred] to the users vault.
     */
    override suspend fun saveCredential(cred: Cipher) {
        val userId = getActiveUserIdOrThrow()

        vaultSdkSource
            .decryptCipher(userId, cred)
            .map { decryptedCipherView ->
                decryptedCipherView.id
                    ?.let { vaultRepository.updateCipher(it, decryptedCipherView) }
                    ?: vaultRepository.createCipher(decryptedCipherView)
            }
            .onFailure { throw it }
    }

    private fun getActiveUserIdOrThrow() = authRepository.userStateFlow.value?.activeUserId
        ?: throw IllegalStateException("Active user is required.")

    /**
     * Return a filtered list containing elements that match the given [relyingPartyId] and a
     * credential ID contained in [credentialIds].
     */
    private fun List<Fido2CredentialAutofillView>.filterMatchingCredentials(
        credentialIds: List<ByteArray>?,
        relyingPartyId: String,
    ): List<Fido2CredentialAutofillView> {
        val skipCredentialIdFiltering = credentialIds.isNullOrEmpty()
        return filter { fido2CredentialView ->
            fido2CredentialView.rpId == relyingPartyId &&
                (skipCredentialIdFiltering ||
                    credentialIds?.contains(fido2CredentialView.credentialId) == true)
        }
    }
}
