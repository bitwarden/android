package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Represents the result of a data export operation.
 */
sealed class ExportDataResult {

    data object Success : ExportDataResult()

    data object Error : ExportDataResult()

}
