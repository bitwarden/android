package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.FolderView

private const val FOLDER_DIVIDER: String = "/"

/**
 * Retrieves the subfolders of a given [folderId] and updates their names to proper display names.
 * This function is necessary if we want to show the subfolders for a specific folder.
 */
fun List<FolderView>.getFolders(folderId: String): List<FolderView> {
    val currentFolder = this.find { it.id == folderId } ?: return emptyList()

    // If two folders have the same name the second folder should have no nested folders
    val firstFolderWithName = this.first { it.name == currentFolder.name }
    if (firstFolderWithName.id != folderId) return emptyList()

    val folderList = this
        .getFilteredFolders(currentFolder.name)
        .map {
            it.copy(name = it.name.substringAfter(currentFolder.name + FOLDER_DIVIDER))
        }

    return folderList
}

/**
 * Filters out subfolders of subfolders from the given list. If a [folderName] is provided,
 * folders that are not subfolders of the specified [folderName] will be filtered out.
 */
fun List<FolderView>.getFilteredFolders(folderName: String? = null): List<FolderView> =
    this.filter { folderView ->
        // If the folder name is not null we filter out folders that are not subfolders.
        if (folderName != null &&
            !folderView.name.startsWith(folderName + FOLDER_DIVIDER)
        ) {
            return@filter false
        }

        this.forEach {
            val firstFolder = folderName
                ?.let { name -> folderView.name.substringAfter(name + FOLDER_DIVIDER) }
                ?: folderView.name

            val secondFolder = folderName
                ?.let { name -> it.name.substringAfter(name + FOLDER_DIVIDER) }
                ?: it.name

            // We don't want to compare the folder to itself or itself plus a slash.
            if (firstFolder == secondFolder || firstFolder == secondFolder + FOLDER_DIVIDER) {
                return@forEach
            }

            // If the first folder name is blank or the first folder is a subfolder of the second
            // folder, we want to filter it out.
            if (firstFolder.isEmpty() ||
                firstFolder.startsWith(secondFolder + FOLDER_DIVIDER)
            ) {
                return@filter false
            }
        }

        true
    }

/**
 * Converts a folder name to a user-friendly display name. This function is necessary because the
 * folder name we receive is often nested, and we want to extract just the relevant name for
 * display to the user.
 */
fun String.toFolderDisplayName(list: List<FolderView>): String {
    var folderName = this

    // cycle through the list and determine the correct display name of the folder.
    list.forEach { folderView ->
        if (this.startsWith(folderView.name + FOLDER_DIVIDER)) {
            val newName = this.substringAfter(folderView.name + FOLDER_DIVIDER)
            if (newName.isNotBlank() && newName.length < folderName.length) {
                folderName = newName
            }
        }
    }

    return folderName
}
