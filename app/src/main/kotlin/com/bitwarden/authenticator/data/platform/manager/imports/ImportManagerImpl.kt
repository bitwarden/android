package com.bitwarden.authenticator.data.platform.manager.imports

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.AegisExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.BitwardenExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.ExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.LastPassExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.TwoFasExportParser

/**
 * Default implementation of [ImportManager] for managing importing files exported by various
 * authenticator applications.
 */
class ImportManagerImpl(
    private val authenticatorDiskSource: AuthenticatorDiskSource,
) : ImportManager {
    override suspend fun import(
        importFileFormat: ImportFileFormat,
        byteArray: ByteArray,
    ): ImportDataResult {

        val parser: ExportParser = when (importFileFormat) {
            ImportFileFormat.BITWARDEN_JSON -> BitwardenExportParser(importFileFormat)
            ImportFileFormat.TWO_FAS_JSON -> TwoFasExportParser()
            ImportFileFormat.LAST_PASS_JSON -> LastPassExportParser()
            ImportFileFormat.AEGIS -> AegisExportParser()
        }

        return try {
            when (val parseResult = parser.parse(byteArray)) {
                is ExportParseResult.Error -> {
                    ImportDataResult.Error(parseResult.message)
                }

                is ExportParseResult.Success -> {
                    authenticatorDiskSource.saveItem(*parseResult.items.toTypedArray())
                    ImportDataResult.Success
                }
            }
        } catch (e: Throwable) {
            ImportDataResult.Error()
        }
    }
}
