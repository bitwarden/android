package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Models result of updating an authenticator item.
 */
sealed class UpdateItemResult {

    /**
     * Item updated successfully.
     */
    data object Success : UpdateItemResult()

    /**
     * Generic error while updating an item. The optional [errorMessage] may be displayed directly
     * in the UI when present.
     */
    data class Error(val errorMessage: String?) : UpdateItemResult()
}
