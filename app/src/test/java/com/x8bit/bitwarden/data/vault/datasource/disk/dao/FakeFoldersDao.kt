package com.x8bit.bitwarden.data.vault.datasource.disk.dao

import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.vault.datasource.disk.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeFoldersDao : FoldersDao {

    val storedFolders = mutableListOf<FolderEntity>()

    var deleteFolderCalled: Boolean = false
    var deleteFoldersCalled: Boolean = false
    var insertFolderCalled: Boolean = false

    private val foldersFlow = bufferedMutableSharedFlow<List<FolderEntity>>(replay = 1)

    init {
        foldersFlow.tryEmit(emptyList())
    }

    override suspend fun deleteAllFolders(userId: String): Int {
        deleteFoldersCalled = true
        val count = storedFolders.count { it.userId == userId }
        storedFolders.removeAll { it.userId == userId }
        foldersFlow.tryEmit(storedFolders.toList())
        return count
    }

    override suspend fun deleteFolder(userId: String, folderId: String) {
        deleteFolderCalled = true
        storedFolders.removeAll { it.userId == userId && it.id == folderId }
        foldersFlow.tryEmit(storedFolders.toList())
    }

    override fun getAllFolders(userId: String): Flow<List<FolderEntity>> =
        foldersFlow.map { ciphers -> ciphers.filter { it.userId == userId } }

    override suspend fun insertFolders(folders: List<FolderEntity>) {
        storedFolders.addAll(folders)
        foldersFlow.tryEmit(storedFolders.toList())
    }

    override suspend fun insertFolder(folder: FolderEntity) {
        storedFolders.add(folder)
        foldersFlow.tryEmit(storedFolders.toList())
        insertFolderCalled = true
    }

    override suspend fun replaceAllFolders(userId: String, folders: List<FolderEntity>): Boolean {
        val removed = storedFolders.removeAll { it.userId == userId }
        storedFolders.addAll(folders)
        foldersFlow.tryEmit(storedFolders.toList())
        return removed || folders.isNotEmpty()
    }
}
