package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.Folder
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson

/**
 * Converts a list of [SyncResponseJson.Folder] objects to a list of corresponding
 * Bitwarden SDK [Folder] objects.
 */
fun List<SyncResponseJson.Folder>.toEncryptedSdkFolderList(): List<Folder> =
    map { it.toEncryptedSdkFolder() }

/**
 * Converts a [SyncResponseJson.Folder] objects to a corresponding
 * Bitwarden SDK [Folder] object.
 */
fun SyncResponseJson.Folder.toEncryptedSdkFolder(): Folder =
    Folder(
        id = id,
        name = name.orEmpty(),
        revisionDate = revisionDate.toInstant(),
    )
