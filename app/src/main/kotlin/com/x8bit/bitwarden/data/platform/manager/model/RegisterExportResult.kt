package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Represents the result of registering for export.
 */
sealed class RegisterExportResult {

    /**
     * Registration was successful.
     */
    data object Success : RegisterExportResult()

    /**
     * Registration failed.
     */
    data class Failure(val throwable: Throwable?) : RegisterExportResult()
}
