package com.bitwarden.authenticator.data.platform.manager.imports.parsers

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult

/**
 * Responsible for transforming exported authenticator data to a format consumable by this
 * application.
 */
interface ExportParser {

    /**
     * Converts the given [byteArray] content of a file to a collection of
     * [AuthenticatorItemEntity].
     */
    fun parse(byteArray: ByteArray): ExportParseResult
}
