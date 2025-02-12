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
     * @property title An optional [Text] providing a brief title of the reason the import failed.
     * @property message An optional [Text] containing an explanation of why the import failed.
     */
    data class Error(
        val title: Text? = null,
        val message: Text? = null,
    ) : ImportDataResult()
}
