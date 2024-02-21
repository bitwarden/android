package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat

/**
 *  Provides a human-readable label for the export format.
 */
val ExportVaultFormat.displayLabel: String
    get() = when (this) {
        ExportVaultFormat.JSON -> ".json"
        ExportVaultFormat.CSV -> ".csv"
        ExportVaultFormat.JSON_ENCRYPTED -> ".json (Encrypted)"
    }

/**
 * Provides the file extension associated with the export format.
 */
val ExportVaultFormat.fileExtension: String
    get() = when (this) {
        ExportVaultFormat.JSON -> "json"
        ExportVaultFormat.CSV -> "csv"
        ExportVaultFormat.JSON_ENCRYPTED -> "json"
    }
