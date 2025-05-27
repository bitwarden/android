package com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model

import com.bitwarden.exporters.ExportFormat

/**
 * Represents the file formats a user can select to export the vault.
 */
enum class ExportVaultFormat {
    JSON,
    CSV,
    JSON_ENCRYPTED,
}

/**
 * Converts the [ExportVaultFormat] to [ExportFormat].
 */
fun ExportVaultFormat.toExportFormat(password: String): ExportFormat =
    when (this) {
        ExportVaultFormat.JSON -> ExportFormat.Json
        ExportVaultFormat.CSV -> ExportFormat.Csv
        ExportVaultFormat.JSON_ENCRYPTED -> ExportFormat.EncryptedJson(password)
    }
