package com.bitwarden.authenticator.data.platform.manager.imports.model

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.ui.platform.base.util.Text

/**
 * Represents the result of parsing an export file.
 */
sealed class ExportParseResult {

    /**
     * Indicates the selected file has been successfully parsed.
     */
    data class Success(val items: List<AuthenticatorItemEntity>) : ExportParseResult()

    /**
     * Represents an error that occurred while parsing the selected file.
     * Provides user-friendly messages to display to the user.
     *
     * @property title A user-friendly title summarizing the error.
     * @property message A detailed message describing the error.
     */
    data class Error(
        val title: Text? = null,
        val message: Text? = null,
    ) : ExportParseResult()
}
