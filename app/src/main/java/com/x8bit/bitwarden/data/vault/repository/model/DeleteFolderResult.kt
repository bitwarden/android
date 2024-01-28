package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Models result of deleting a folder.
 */
sealed class DeleteFolderResult {

    /**
     * Folder deleted successfully.
     */
    data object Success : DeleteFolderResult()

    /**
     * Generic error while deleting a folder.
     */
    data object Error : DeleteFolderResult()
}
