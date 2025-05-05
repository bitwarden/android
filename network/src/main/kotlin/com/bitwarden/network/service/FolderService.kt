package com.bitwarden.network.service

import com.bitwarden.network.model.FolderJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateFolderResponseJson

/**
 * Provides an API for querying folder endpoints.
 */
interface FolderService {
    /**
     * Attempt to create a folder.
     */
    suspend fun createFolder(body: FolderJsonRequest): Result<SyncResponseJson.Folder>

    /**
     * Attempt to update a folder.
     */
    suspend fun updateFolder(
        folderId: String,
        body: FolderJsonRequest,
    ): Result<UpdateFolderResponseJson>

    /**
     * Attempt to hard delete a folder.
     */
    suspend fun deleteFolder(folderId: String): Result<Unit>

    /**
     * Attempt to retrieve a folder.
     */
    suspend fun getFolder(folderId: String): Result<SyncResponseJson.Folder>
}
