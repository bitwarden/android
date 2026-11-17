package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.sdk.CipherIterator
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A user-scoped implementation of a Bitwarden SDK [CipherIterator] that reads the ciphers of the
 * user with the given [userId] from disk, up to [batchSize] per [next] call, ordered by id.
 *
 * Each call runs its own query for the ciphers after the id of the last cipher read, so no
 * database cursor or transaction stays open between calls. A cipher saved or deleted between
 * calls is included or left out depending on whether its id sorts after the last cipher read.
 */
class SdkCipherIterator(
    private val userId: String,
    private val vaultDiskSource: VaultDiskSource,
    private val batchSize: Int,
) : CipherIterator {
    // The SDK calls `next` one at a time, but each call can run on a different thread, so the
    // mutex makes the state of one call visible to the next.
    private val mutex = Mutex()
    private var lastCipherId: String? = null
    private var isDone = false

    override suspend fun next(): List<Cipher>? = mutex.withLock {
        if (isDone) return@withLock null
        val page = vaultDiskSource.getCiphersPage(
            userId = userId,
            afterCipherId = lastCipherId,
            limit = batchSize,
        )
        // A page shorter than the batch size is the last one.
        isDone = page.size < batchSize
        if (page.isEmpty()) return@withLock null
        lastCipherId = page.last().id
        page.map { it.toEncryptedSdkCipher() }
    }
}
