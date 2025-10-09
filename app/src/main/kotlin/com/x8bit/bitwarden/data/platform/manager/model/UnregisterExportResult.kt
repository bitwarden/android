package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Represents the result of unregistering for Credential Exchange Protocol export.
 */
sealed class UnregisterExportResult {
    /**
     * Represents a successful unregistering for Credential Exchange Protocol export.
     */
    data object Success : UnregisterExportResult()

    /**
     * Represents a failure to unregister for Credential Exchange Protocol export.
     */
    data class Failure(val throwable: Throwable?) : UnregisterExportResult()
}
