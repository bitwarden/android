package com.x8bit.bitwarden.data.vault.repository.model

/**
 * Represents the result of a sync operation.
 */
sealed class SyncVaultDataResult {
    /**
     * Indicates a successful sync operation.
     */
    data object Success : SyncVaultDataResult()

    /**
     * Indicates a failed sync operation.
     *
     * @property throwable The exception that caused the failure, if any.
     */
    data class Error(val throwable: Throwable?) : SyncVaultDataResult()
}
