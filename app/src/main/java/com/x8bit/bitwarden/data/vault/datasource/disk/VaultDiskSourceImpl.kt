package com.x8bit.bitwarden.data.vault.datasource.disk

import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Default implementation of [VaultDiskSource].
 */
class VaultDiskSourceImpl(
    private val ciphersDao: CiphersDao,
    private val json: Json,
) : VaultDiskSource {

    override fun getCiphers(
        userId: String,
    ): Flow<List<SyncResponseJson.Cipher>> =
        ciphersDao
            .getAllCiphers(userId = userId)
            .map { entities ->
                entities.map { entity ->
                    json.decodeFromString<SyncResponseJson.Cipher>(entity.cipherJson)
                }
            }

    override suspend fun replaceVaultData(userId: String, vault: SyncResponseJson) {
        ciphersDao.replaceAllCiphers(
            userId = userId,
            ciphers = vault.ciphers.orEmpty().map { cipher ->
                CipherEntity(
                    id = cipher.id,
                    userId = userId,
                    cipherType = json.encodeToString(cipher.type),
                    cipherJson = json.encodeToString(cipher),
                )
            },
        )
    }

    override suspend fun deleteVaultData(userId: String) {
        ciphersDao.deleteAllCiphers(userId = userId)
    }
}
