package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Represents the result of a data export operation.
 */
sealed class ExportDataResult {

    /**
     * Data has been successfully exported.
     */
    data object Success : ExportDataResult()

    /**
     * Data could not be exported.
     */
    data object Error : ExportDataResult()
}
