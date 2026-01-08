package com.x8bit.bitwarden.data.vault.datasource.disk

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.decodeFromStringWithErrorCallback
import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CiphersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.CollectionsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.DomainsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.FoldersDao
import com.x8bit.bitwarden.data.vault.datasource.disk.dao.SendsDao
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CipherEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.CollectionEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.DomainsEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.SendEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Default implementation of [VaultDiskSource].
 */
@Suppress("TooManyFunctions", "LongParameterList")
class VaultDiskSourceImpl(
    private val ciphersDao: CiphersDao,
    private val collectionsDao: CollectionsDao,
    private val domainsDao: DomainsDao,
    private val foldersDao: FoldersDao,
    private val sendsDao: SendsDao,
    private val json: Json,
    private val dispatcherManager: DispatcherManager,
) : VaultDiskSource {

    private val forceCiphersFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Cipher>>()
    private val forceCollectionsFlow =
        bufferedMutableSharedFlow<List<SyncResponseJson.Collection>>()
    private val forceFolderFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Folder>>()
    private val forceSendFlow = bufferedMutableSharedFlow<List<SyncResponseJson.Send>>()

    override suspend fun saveCipher(userId: String, cipher: SyncResponseJson.Cipher) {
        ciphersDao.insertCiphers(
            ciphers = listOf(
                CipherEntity(
                    id = cipher.id,
                    userId = userId,
                    hasTotp = cipher.login?.totp != null,
                    cipherType = json.encodeToString(cipher.type),
                    cipherJson = json.encodeToString(cipher),
                    organizationId = cipher.organizationId,
                ),
            ),
        )
    }

    override fun getCiphersFlow(
        userId: String,
    ): Flow<List<SyncResponseJson.Cipher>> =
        merge(
            forceCiphersFlow,
            ciphersDao
                .getAllCiphersFlow(userId = userId)
                .map { entities ->
                    withContext(context = dispatcherManager.default) {
                        entities
                            .map { entity ->
                                async {
                                    json.decodeFromStringWithErrorCallback<SyncResponseJson.Cipher>(
                                        string = entity.cipherJson,
                                    ) { Timber.e(it, "Failed to deserialize Cipher in Vault") }
                                }
                            }
                            .awaitAll()
                    }
                },
        )

    override suspend fun getCiphers(userId: String): List<SyncResponseJson.Cipher> {
        val entities = ciphersDao.getAllCiphers(userId = userId)
        return withContext(context = dispatcherManager.default) {
            entities
                .map { entity ->
                    async {
                        json.decodeFromStringWithErrorCallback<SyncResponseJson.Cipher>(
                            string = entity.cipherJson,
                        ) { Timber.e(it, "Failed to deserialize Cipher in Vault") }
                    }
                }
                .awaitAll()
        }
    }

    override fun hasPersonalCiphersFlow(userId: String): Flow<Boolean> =
        ciphersDao.hasPersonalCiphersFlow(userId = userId)

    override suspend fun getSelectedCiphers(
        userId: String,
        cipherIds: List<String>,
    ): List<SyncResponseJson.Cipher> {
        val entities = ciphersDao.getSelectedCiphers(userId = userId, cipherIds = cipherIds)
        return withContext(context = dispatcherManager.default) {
            entities
                .map { entity ->
                    async {
                        json.decodeFromStringWithErrorCallback<SyncResponseJson.Cipher>(
                            string = entity.cipherJson,
                        ) { Timber.e(it, "Failed to deserialize Cipher in Vault") }
                    }
                }
                .awaitAll()
        }
    }

    override suspend fun getTotpCiphers(userId: String): List<SyncResponseJson.Cipher> {
        val entities = ciphersDao.getAllTotpCiphers(userId = userId)
        return withContext(context = dispatcherManager.default) {
            entities
                .map { entity ->
                    async {
                        json.decodeFromStringWithErrorCallback<SyncResponseJson.Cipher>(
                            string = entity.cipherJson,
                        ) { Timber.e(it, "Failed to deserialize TOTP Cipher in Vault") }
                    }
                }
                .awaitAll()
                .filter {
                    // A safety-check since after the DB migration, we will temporarily think
                    // all ciphers contain a totp code
                    it.login?.totp != null
                }
        }
    }

    override suspend fun getCipher(
        userId: String,
        cipherId: String,
    ): SyncResponseJson.Cipher? =
        ciphersDao
            .getCipher(userId = userId, cipherId = cipherId)
            ?.let { entity ->
                withContext(context = dispatcherManager.default) {
                    json.decodeFromStringWithErrorCallback<SyncResponseJson.Cipher>(
                        string = entity.cipherJson,
                    ) { Timber.e(it, "Failed to deserialize Cipher in Vault") }
                }
            }

    override suspend fun deleteCipher(userId: String, cipherId: String) {
        ciphersDao.deleteCipher(userId, cipherId)
    }

    override suspend fun saveCollection(userId: String, collection: SyncResponseJson.Collection) {
        collectionsDao.insertCollection(
            collection = CollectionEntity(
                userId = userId,
                id = collection.id,
                name = collection.name,
                organizationId = collection.organizationId,
                shouldHidePasswords = collection.shouldHidePasswords,
                externalId = collection.externalId,
                isReadOnly = collection.isReadOnly,
                canManage = collection.canManage,
                defaultUserCollectionEmail = collection.defaultUserCollectionEmail,
                type = json.encodeToString(collection.type),
            ),
        )
    }

    override fun getCollections(
        userId: String,
    ): Flow<List<SyncResponseJson.Collection>> =
        merge(
            forceCollectionsFlow,
            collectionsDao
                .getAllCollections(userId = userId)
                .map { entities ->
                    entities.map { entity ->
                        SyncResponseJson.Collection(
                            id = entity.id,
                            name = entity.name,
                            organizationId = entity.organizationId,
                            shouldHidePasswords = entity.shouldHidePasswords,
                            externalId = entity.externalId,
                            isReadOnly = entity.isReadOnly,
                            canManage = entity.canManage,
                            defaultUserCollectionEmail = entity.defaultUserCollectionEmail,
                            type = json.decodeFromString(entity.type),
                        )
                    }
                },
        )

    override fun getDomains(userId: String): Flow<SyncResponseJson.Domains?> =
        domainsDao
            .getDomains(userId)
            .map { entity ->
                withContext(dispatcherManager.default) {
                    entity?.domainsJson?.let { domains ->
                        json.decodeFromStringWithErrorCallback<SyncResponseJson.Domains>(
                            string = domains,
                        ) { Timber.e(it, "Failed to deserialize Domains in Vault") }
                    }
                }
            }

    override suspend fun deleteFolder(userId: String, folderId: String) {
        foldersDao.deleteFolder(userId = userId, folderId = folderId)
    }

    override suspend fun saveFolder(userId: String, folder: SyncResponseJson.Folder) {
        foldersDao.insertFolder(
            folder = FolderEntity(
                userId = userId,
                id = folder.id,
                name = folder.name,
                revisionDate = folder.revisionDate,
            ),
        )
    }

    override fun getFolders(
        userId: String,
    ): Flow<List<SyncResponseJson.Folder>> =
        merge(
            forceFolderFlow,
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
                },
        )

    override suspend fun saveSend(userId: String, send: SyncResponseJson.Send) {
        sendsDao.insertSends(
            sends = listOf(
                SendEntity(
                    userId = userId,
                    id = send.id,
                    sendType = json.encodeToString(send.type),
                    sendJson = json.encodeToString(send),
                ),
            ),
        )
    }

    override suspend fun deleteSend(userId: String, sendId: String) {
        sendsDao.deleteSend(userId, sendId)
    }

    override fun getSends(
        userId: String,
    ): Flow<List<SyncResponseJson.Send>> =
        merge(
            forceSendFlow,
            sendsDao
                .getAllSends(userId = userId)
                .map { entities ->
                    withContext(context = dispatcherManager.default) {
                        entities
                            .map { entity ->
                                async {
                                    json.decodeFromStringWithErrorCallback<SyncResponseJson.Send>(
                                        string = entity.sendJson,
                                    ) { Timber.e(it, "Failed to deserialize Send in Vault") }
                                }
                            }
                            .awaitAll()
                    }
                },
        )

    @Suppress("LongMethod")
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
                            hasTotp = cipher.login?.totp != null,
                            cipherType = json.encodeToString(cipher.type),
                            cipherJson = json.encodeToString(cipher),
                            organizationId = cipher.organizationId,
                        )
                    },
                )
            }
            val deferredCollections = async {
                collectionsDao.replaceAllCollections(
                    userId = userId,
                    collections = vault.collections.orEmpty().map { collection ->
                        CollectionEntity(
                            userId = userId,
                            id = collection.id,
                            name = collection.name,
                            organizationId = collection.organizationId,
                            shouldHidePasswords = collection.shouldHidePasswords,
                            externalId = collection.externalId,
                            isReadOnly = collection.isReadOnly,
                            canManage = collection.canManage,
                            defaultUserCollectionEmail = collection.defaultUserCollectionEmail,
                            type = json.encodeToString(collection.type),
                        )
                    },
                )
            }
            launch {
                domainsDao.insertDomains(
                    domains = DomainsEntity(
                        userId = userId,
                        domainsJson = vault.domains?.let { json.encodeToString(it) },
                    ),
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
            val deferredSends = async {
                sendsDao.replaceAllSends(
                    userId = userId,
                    sends = vault.sends.orEmpty().map { send ->
                        SendEntity(
                            userId = userId,
                            id = send.id,
                            sendType = json.encodeToString(send.type),
                            sendJson = json.encodeToString(send),
                        )
                    },
                )
            }
            // When going from 0 items to 0 items, the respective dao flow will not re-emit
            // So we use this to give it a little push.
            if (!deferredCiphers.await()) {
                forceCiphersFlow.tryEmit(emptyList())
            }
            if (!deferredCollections.await()) {
                forceCollectionsFlow.tryEmit(emptyList())
            }
            if (!deferredFolders.await()) {
                forceFolderFlow.tryEmit(emptyList())
            }
            if (!deferredSends.await()) {
                forceSendFlow.tryEmit(emptyList())
            }
        }
    }

    override suspend fun resyncVaultData(userId: String) {
        coroutineScope {
            val deferredCiphers = async { getCiphersFlow(userId = userId).first() }
            val deferredCollections = async { getCollections(userId = userId).first() }
            val deferredFolders = async { getFolders(userId = userId).first() }
            val deferredSends = async { getSends(userId = userId).first() }

            forceCiphersFlow.tryEmit(deferredCiphers.await())
            forceCollectionsFlow.tryEmit(deferredCollections.await())
            forceFolderFlow.tryEmit(deferredFolders.await())
            forceSendFlow.tryEmit(deferredSends.await())
        }
    }

    override suspend fun deleteVaultData(userId: String) {
        coroutineScope {
            val deferredCiphers = async { ciphersDao.deleteAllCiphers(userId = userId) }
            val deferredCollections = async { collectionsDao.deleteAllCollections(userId = userId) }
            val deferredDomains = async { domainsDao.deleteDomains(userId = userId) }
            val deferredFolders = async { foldersDao.deleteAllFolders(userId = userId) }
            val deferredSends = async { sendsDao.deleteAllSends(userId = userId) }
            awaitAll(
                deferredCiphers,
                deferredCollections,
                deferredDomains,
                deferredFolders,
                deferredSends,
            )
        }
    }
}
