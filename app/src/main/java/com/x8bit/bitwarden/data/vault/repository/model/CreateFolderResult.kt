package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.vault.FolderView

/**
 * Models result of creating a folder.
 */
sealed class CreateFolderResult {

    /**
     * Folder created successfully.
     */
    data class Success(val folderView: FolderView) : CreateFolderResult()

    /**
     * Generic error while creating a folder.
     */
    data object Error : CreateFolderResult()
}
