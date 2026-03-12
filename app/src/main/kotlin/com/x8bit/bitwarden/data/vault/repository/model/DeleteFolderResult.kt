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
     * Generic error while deleting a folder. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = null,
    ) : DeleteFolderResult()
}
