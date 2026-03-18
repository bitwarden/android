package com.x8bit.bitwarden.data.vault.repository.model

import com.bitwarden.collections.CollectionView
import com.x8bit.bitwarden.data.platform.util.userFriendlyMessage

/**
 * Models result of creating a collection.
 */
sealed class CreateCollectionResult {

    /**
     * Collection created successfully.
     */
    data class Success(val collectionView: CollectionView) : CreateCollectionResult()

    /**
     * Generic error while creating a collection. The optional [errorMessage] may be displayed
     * directly in the UI when present.
     */
    data class Error(
        val error: Throwable,
        val errorMessage: String? = error.userFriendlyMessage,
    ) : CreateCollectionResult()
}
