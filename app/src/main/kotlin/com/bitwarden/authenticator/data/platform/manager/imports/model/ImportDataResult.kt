package com.bitwarden.authenticator.data.platform.manager.imports.model

import com.bitwarden.authenticator.ui.platform.base.util.Text

/**
 * Represents the result of a data import operation.
 */
sealed class ImportDataResult {
    /**
     * Indicates import was successful.
     */
    data object Success : ImportDataResult()

    /**
     * Indicates import was not successful.
     *
     * @property message Optional [Text] indicating why the import failed.
     */
    data class Error(val message: Text? = null) : ImportDataResult()
}
