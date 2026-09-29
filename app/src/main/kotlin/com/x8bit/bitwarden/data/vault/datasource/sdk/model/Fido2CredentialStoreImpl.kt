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
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

private const val B64_PREFIX: String = "b64."
private const val UUID_BYTE_COUNT: Int = 16

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
            .onSuccess { decryptedCipherView ->
                val result = decryptedCipherView.id
                    ?.let {
                        vaultRepository
                            .updateCipher(it, decryptedCipherView)
                            .toCreateCipherResult(cipherId = it)
                    }
                    ?: decryptedCipherView.createCipher()

                when (result) {
                    is CreateCipherResult.Success -> Unit
                    is CreateCipherResult.Error -> {
                        throw result.error ?: IllegalStateException(
                            result.errorMessage ?: "Failed to save credential",
                        )
                    }
                }
            }
            .onFailure { throw it }
    }

    private suspend fun CipherView.createCipher(): CreateCipherResult {
        val collectionIds = this.collectionIds
        return if (this.organizationId != null && collectionIds.isNotEmpty()) {
            vaultRepository.createCipherInOrganization(
                cipherView = this,
                collectionIds = collectionIds,
            )
        } else {
            vaultRepository.createCipher(cipherView = this)
        }
    }

    private fun UpdateCipherResult.toCreateCipherResult(cipherId: String): CreateCipherResult =
        when (this) {
            UpdateCipherResult.Success -> CreateCipherResult.Success(cipherId = cipherId)
            is UpdateCipherResult.Error -> CreateCipherResult.Error(
                error = error,
                errorMessage = errorMessage,
            )
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
                .mapNotNull { it.credentialId.toGuidBytesOrNull() }

            val hasIntersectingCredentials = credentialIds
                .orEmpty()
                .any { id -> fido2CredentialIds.any { it.contentEquals(id) } }

            hasMatchingRpId &&
                (skipCredentialIdFiltering || hasIntersectingCredentials)
        }
    }

    /**
     * Convert a stored credential ID to its raw bytes, mirroring the SDK's
     * `string_to_guid_bytes`. Ids prefixed with "b64." are Base64 URL-safe encoded; all others
     * are UUIDs (16 big-endian bytes). Returns null when the id is invalid.
     */
    private fun String.toGuidBytesOrNull(): ByteArray? {
        if (startsWith(B64_PREFIX)) {
            return runCatching { Base64.getUrlDecoder().decode(removePrefix(B64_PREFIX)) }
                .getOrNull()
        }

        val uuid = runCatching { UUID.fromString(this) }.getOrNull() ?: return null
        return ByteBuffer
            .allocate(UUID_BYTE_COUNT)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
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
