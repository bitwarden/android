package com.bitwarden.authenticator.data.platform.manager.imports.model

import com.bitwarden.ui.util.Text

/**
 * Represents the result of a data import operation.
 */
sealed class ImportDataResult {
    /**
     * Indicates import was successful.
     */
    data object Success : ImportDataResult()

    /**
     * Indicates the selected file is password protected and a password is needed to continue.
     */
    data object PasswordRequired : ImportDataResult()

    /**
     * Indicates the password provided for a password-protected file was incorrect.
     */
    data object IncorrectPassword : ImportDataResult()

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
