package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import com.bitwarden.sdk.CipherListItem
import com.bitwarden.sdk.CipherListStream
import com.bitwarden.sdk.CiphersClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits every cipher in the user's local state as a [CipherListItem], in the order of the cipher
 * repository, read and decrypted up to [chunkSize] ciphers at a time (the SDK default when
 * `null`).
 *
 * Each collection opens its own [CipherListStream] and closes it when the collection ends, and
 * asks the stream for the next chunk only after every item of the previous chunk was emitted, so
 * a collector that stops early stops the reading. The flow fails with the error of the stream,
 * for example a [com.bitwarden.sdk.BitwardenException.DecryptStream] when the vault is locked.
 */
fun CiphersClient.listStreamFlow(chunkSize: UInt? = null): Flow<CipherListItem> = flow {
    listStream(chunkSize = chunkSize).use { stream ->
        while (true) {
            val chunk = stream.nextChunk() ?: break
            chunk.forEach { emit(it) }
        }
    }
}
