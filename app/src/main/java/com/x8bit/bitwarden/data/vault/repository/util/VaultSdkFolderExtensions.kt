package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.platform.util.SpecialCharWithPrecedenceComparator
import com.x8bit.bitwarden.data.vault.datasource.network.model.FolderJsonRequest
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

/**
 * Converts a Bitwarden SDK [Folder] objects to a corresponding
 * [SyncResponseJson.Folder] object.
 */
fun Folder.toEncryptedNetworkFolder(): FolderJsonRequest =
    FolderJsonRequest(name = name)

/**
 * Sorts the data in alphabetical order by name.
 */
@JvmName("toAlphabeticallySortedFolderList")
fun List<FolderView>.sortAlphabetically(): List<FolderView> {
    return this.sortedWith(
        comparator = { folder1, folder2 ->
            SpecialCharWithPrecedenceComparator.compare(folder1.name, folder2.name)
        },
    )
}
