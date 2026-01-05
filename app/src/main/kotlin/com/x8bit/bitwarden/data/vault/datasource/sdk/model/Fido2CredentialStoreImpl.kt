package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.sdk.Fido2CredentialStore
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.EncryptionContext
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import timber.log.Timber

/**
 * Primary implementation of [Fido2CredentialStore].
 */
@OmitFromCoverage
class Fido2CredentialStoreImpl(
    private val authRepository: AuthRepository,
    private val vaultSdkSource: VaultSdkSource,
    private val vaultRepository: VaultRepository,
) : Fido2CredentialStore {

    /**
     * Return all active ciphers that contain FIDO 2 credentials.
     */
    override suspend fun allCredentials(): List<CipherListView> = vaultRepository
        .decryptCipherListResultStateFlow
        .value
        .data
        ?.successes
        .orEmpty()
        .filter { it.isActiveWithFido2Credentials }

    /**
     * Returns ciphers that contain FIDO 2 credentials for the given [ripId] with the provided
     * [ids].
     *
     * @param ids Optional list of FIDO 2 credential ID's to find.
     * @param ripId Relying Party ID to find.
     */
    override suspend fun findCredentials(
        ids: List<ByteArray>?,
        ripId: String,
        userHandle: ByteArray?,
    ): List<CipherView> =
        vaultRepository
            .decryptCipherListResultStateFlow
            .value
            .data
            ?.successes
            .orEmpty()
            .filter { it.isActiveWithFido2Credentials }
            .filterMatchingCredentials(
                credentialIds = ids,
                relyingPartyId = ripId,
            )
            .mapNotNull { cipherListView ->
                cipherListView.id
                    ?.let { cipherId ->
                        vaultRepository
                            .getCipher(cipherId = cipherId)
                            .toCipherViewOrNull()
                    }
            }

    /**
     * Save the provided [cred] to the users vault.
     */
    override suspend fun saveCredential(cred: EncryptionContext) {
        vaultSdkSource
            .decryptCipher(
                userId = authRepository.activeUserId ?: throw NoActiveUserException(),
                cipher = cred.cipher,
            )
            .map { decryptedCipherView ->
                decryptedCipherView.id
                    ?.let { vaultRepository.updateCipher(it, decryptedCipherView) }
                    ?: vaultRepository.createCipher(decryptedCipherView)
            }
            .onFailure { throw it }
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

            val hasIntersectingCredentials = credentialIds
                ?.intersect(fido2CredentialIds)
                .orEmpty()
                .isNotEmpty()

            hasMatchingRpId &&
                (skipCredentialIdFiltering || hasIntersectingCredentials)
        }
    }

    private fun GetCipherResult.toCipherViewOrNull(): CipherView? {
        return when (this) {
            GetCipherResult.CipherNotFound -> {
                Timber.e("Cipher not found for FIDO 2 credential.")
                null
            }

            is GetCipherResult.Failure -> {
                Timber.e(this.error, "Failed to decrypt cipher for FIDO 2 credential.")
                null
            }

            is GetCipherResult.Success -> this.cipherView
        }
    }
}
