package com.bitwarden.network.service

import com.bitwarden.network.api.FoldersApi
import com.bitwarden.network.model.FolderJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateFolderResponseJson
import com.bitwarden.network.model.toBitwardenError
import com.bitwarden.network.util.NetworkErrorCode
import com.bitwarden.network.util.parseErrorBodyOrNull
import com.bitwarden.network.util.toResult
import kotlinx.serialization.json.Json

internal class FolderServiceImpl(
    private val foldersApi: FoldersApi,
    private val json: Json,
) : FolderService {
    override suspend fun createFolder(body: FolderJsonRequest): Result<SyncResponseJson.Folder> =
        foldersApi
            .createFolder(body = body)
            .toResult()

    override suspend fun updateFolder(
        folderId: String,
        body: FolderJsonRequest,
    ): Result<UpdateFolderResponseJson> =
        foldersApi
            .updateFolder(
                folderId = folderId,
                body = body,
            )
            .toResult()
            .map { UpdateFolderResponseJson.Success(folder = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateFolderResponseJson.Invalid>(
                        code = NetworkErrorCode.BAD_REQUEST,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun deleteFolder(folderId: String): Result<Unit> =
        foldersApi
            .deleteFolder(folderId = folderId)
            .toResult()

    override suspend fun getFolder(
        folderId: String,
    ): Result<SyncResponseJson.Folder> = foldersApi
        .getFolder(folderId = folderId)
        .toResult()
}
