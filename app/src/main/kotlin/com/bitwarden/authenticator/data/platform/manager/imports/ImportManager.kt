package com.bitwarden.authenticator.data.platform.manager.imports

import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat

/**
 * Responsible for managing import of files from various authenticator exports.
 */
interface ImportManager {

    /**
     * Imports the selected file.
     */
    suspend fun import(
        importFileFormat: ImportFileFormat,
        byteArray: ByteArray,
    ): ImportDataResult

}
