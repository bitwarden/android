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
     * Indicates there was an error while parsing the selected file.
     *
     * @property message User friendly message displayed to the user, if provided.
     */
    data class Error(val message: Text? = null) : ExportParseResult()
}
