package com.bitwarden.authenticator.data.authenticator.repository.model

/**
 * Represents the result of a data import operation.
 */
sealed class ImportDataResult {
    data object Success : ImportDataResult()

    data object Error : ImportDataResult()
}
