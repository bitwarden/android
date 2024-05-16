package com.bitwarden.authenticator.data.platform.manager.imports.model

/**
 * Represents the result of a data import operation.
 */
sealed class ImportDataResult {
    data object Success : ImportDataResult()

    data object Error : ImportDataResult()
}
