package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.vault.FolderView

/**
 * Models result of updating a folder.
 */
sealed class UpdateFolderResult {

    /**
     * Folder updated successfully.
     */
    data class Success(val folderView: FolderView) : UpdateFolderResult()

    /**
     * Generic error while updating a folder. The optional [errorMessage]
     * may be displayed directly in the UI when present.
     */
    data class Error(val errorMessage: String?) : UpdateFolderResult()
}
