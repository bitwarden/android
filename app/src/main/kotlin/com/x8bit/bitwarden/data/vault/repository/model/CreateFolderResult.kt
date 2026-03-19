package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of creating a folder.
 */
sealed class CreateFolderResult {

    /**
     * Folder created successfully.
     */
    data class Success(val folderView: FolderView) : CreateFolderResult()

    /**
     * Generic error while creating a folder. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : CreateFolderResult()
}
