package com.x8bit.bitwarden.data.vault.repository.model

import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of deleting a collection.
 */
sealed class DeleteCollectionResult {

    /**
     * Collection deleted successfully.
     */
    data object Success : DeleteCollectionResult()

    /**
     * Generic error while deleting a collection. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : DeleteCollectionResult()
}
