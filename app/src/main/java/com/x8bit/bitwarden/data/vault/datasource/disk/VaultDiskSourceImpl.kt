package com.x8bit.bitwarden.data.vault.datasource.disk

import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Default implementation of [VaultDiskSource].
 */
class VaultDiskSourceImpl(
    private val ciphersDao: CiphersDao,
    private val foldersDao: FoldersDao,
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

    override fun getFolders(
        userId: String,
    ): Flow<List<SyncResponseJson.Folder>> =
        foldersDao
            .getAllFolders(userId = userId)
            .map { entities ->
                entities.map { entity ->
                    SyncResponseJson.Folder(
                        id = entity.id,
                        name = entity.name,
                        revisionDate = entity.revisionDate,
                    )
                }
            }

    override suspend fun replaceVaultData(
        userId: String,
        vault: SyncResponseJson,
    ) {
        coroutineScope {
            val deferredCiphers = async {
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
            val deferredFolders = async {
                foldersDao.replaceAllFolders(
                    userId = userId,
                    folders = vault.folders.orEmpty().map { folder ->
                        FolderEntity(
                            userId = userId,
                            id = folder.id,
                            name = folder.name,
                            revisionDate = folder.revisionDate,
                        )
                    },
                )
            }
            awaitAll(
                deferredCiphers,
                deferredFolders,
            )
        }
    }

    override suspend fun deleteVaultData(userId: String) {
        coroutineScope {
            val deferredCiphers = async { ciphersDao.deleteAllCiphers(userId = userId) }
            val deferredFolders = async { foldersDao.deleteAllFolders(userId = userId) }
            awaitAll(
                deferredCiphers,
                deferredFolders,
            )
        }
    }
}
