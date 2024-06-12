package com.bitwarden.authenticator.data.platform.manager.imports.model

import com.bitwarden.authenticator.ui.platform.base.util.Text

/**
 * Represents the result of a data import operation.
 */
sealed class ImportDataResult {
    data object Success : ImportDataResult()

    data class Error(val message: Text? = null) : ImportDataResult()
}
