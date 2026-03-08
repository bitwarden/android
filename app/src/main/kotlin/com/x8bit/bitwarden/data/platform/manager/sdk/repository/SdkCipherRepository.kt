package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.sdk.CipherRepository
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipherResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import timber.log.Timber

/**
 * A user-scoped implementation of a Bitwarden SDK [CipherRepository].
 */
class SdkCipherRepository(
    private val userId: String,
    private val vaultDiskSource: VaultDiskSource,
) : CipherRepository {
    override suspend fun get(id: String): Cipher? =
        vaultDiskSource
            .getCipher(userId = userId, cipherId = id)
            ?.toEncryptedSdkCipher()

    override suspend fun has(id: String): Boolean = this.get(id = id) != null

    override suspend fun list(): List<Cipher> =
        vaultDiskSource
            .getCiphers(userId = userId)
            .map { it.toEncryptedSdkCipher() }

    override suspend fun remove(id: String) {
        vaultDiskSource.deleteCipher(userId = userId, cipherId = id)
    }

    override suspend fun set(id: String, value: Cipher) {
        if (id != value.id) {
            Timber.e("SDK Cipher 'set' operation: ID's do not match")
            return
        }
        vaultDiskSource.saveCipher(
            userId = userId,
            cipher = value.toEncryptedNetworkCipherResponse(encryptedFor = userId),
        )
    }

    override suspend fun setBulk(values: Map<String, Cipher>) {
        val validEntries = values.filter { (id, cipher) ->
            if (id != cipher.id) {
                Timber.e(
                    "SDK Cipher 'setBulk' operation: ID's do not match for '$id'",
                )
                false
            } else {
                true
            }
        }
        if (validEntries.isEmpty()) return
        vaultDiskSource.saveCiphers(
            userId = userId,
            ciphers = validEntries.values.map {
                it.toEncryptedNetworkCipherResponse(encryptedFor = userId)
            },
        )
    }

    override suspend fun removeBulk(keys: List<String>) {
        if (keys.isEmpty()) return
        vaultDiskSource.deleteSelectedCiphers(userId = userId, cipherIds = keys)
    }

    override suspend fun removeAll() {
        vaultDiskSource.deleteAllCiphers(userId = userId)
    }
}
