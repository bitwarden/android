package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.collections.CollectionView
import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of updating a collection.
 */
sealed class UpdateCollectionResult {

    /**
     * Collection updated successfully.
     */
    data class Success(val collectionView: CollectionView) : UpdateCollectionResult()

    /**
     * Generic error while updating a collection. The optional [errorMessage]
     * may be displayed directly in the UI when present.
     */
    data class Error(
        val error: Throwable? = null,
        val errorMessage: String? = error?.userFriendlyMessage,
    ) : UpdateCollectionResult()
}
