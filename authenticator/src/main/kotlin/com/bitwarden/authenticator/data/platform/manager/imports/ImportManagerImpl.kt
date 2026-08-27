package com.bitwarden.authenticator.data.platform.manager.imports

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.platform.manager.crypto.ExportEncryptionManager
import com.bitwarden.authenticator.data.platform.manager.crypto.model.DecryptExportResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ExportParseResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.AegisExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.BitwardenExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.ExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.LastPassExportParser
import com.bitwarden.authenticator.data.platform.manager.imports.parsers.TwoFasExportParser
import com.bitwarden.core.data.manager.UuidManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText

/**
 * Default implementation of [ImportManager] for managing importing files exported by various
 * authenticator applications.
 */
class ImportManagerImpl(
    private val authenticatorDiskSource: AuthenticatorDiskSource,
    private val exportEncryptionManager: ExportEncryptionManager,
    private val uuidManager: UuidManager,
) : ImportManager {
    override suspend fun import(
        importFileFormat: ImportFileFormat,
        byteArray: ByteArray,
        password: String?,
    ): ImportDataResult {
        // Only the Bitwarden JSON format can be password protected.
        if (importFileFormat == ImportFileFormat.BITWARDEN_JSON) {
            val content = byteArray.decodeToString()
            if (exportEncryptionManager.isPasswordProtected(json = content)) {
                return importPasswordProtected(content = content, password = password)
            }
        }
        val parser = createParser(importFileFormat)
        return processParseResult(parser.parseForResult(byteArray))
    }

    private suspend fun importPasswordProtected(
        content: String,
        password: String?,
    ): ImportDataResult {
        if (password == null) return ImportDataResult.PasswordRequired
        val result = exportEncryptionManager.decrypt(json = content, password = password)
        return when (result) {
            DecryptExportResult.IncorrectPassword -> ImportDataResult.IncorrectPassword

            DecryptExportResult.UnsupportedKdf -> ImportDataResult.Error(
                message = BitwardenString.the_file_uses_an_unsupported_encryption_setting.asText(),
            )

            DecryptExportResult.Error -> ImportDataResult.Error(
                title = BitwardenString.file_could_not_be_processed.asText(),
                message = BitwardenString.file_could_not_be_processed_message.asText(),
            )

            is DecryptExportResult.Success -> processParseResult(
                BitwardenExportParser(ImportFileFormat.BITWARDEN_JSON)
                    .parseForResult(result.json.toByteArray()),
            )
        }
    }

    private fun createParser(
        importFileFormat: ImportFileFormat,
    ): ExportParser = when (importFileFormat) {
        ImportFileFormat.BITWARDEN_JSON -> BitwardenExportParser(importFileFormat)
        ImportFileFormat.TWO_FAS_JSON -> TwoFasExportParser(uuidManager)
        ImportFileFormat.LAST_PASS_JSON -> LastPassExportParser(uuidManager)
        ImportFileFormat.AEGIS -> AegisExportParser(uuidManager)
    }

    private suspend fun processParseResult(
        parseResult: ExportParseResult,
    ): ImportDataResult = when (parseResult) {
        is ExportParseResult.Error -> {
            ImportDataResult.Error(
                title = parseResult.title,
                message = parseResult.message,
            )
        }

        is ExportParseResult.Success -> {
            val items = parseResult.items.toTypedArray()
            authenticatorDiskSource.saveItem(*items)
            ImportDataResult.Success
        }
    }
}
