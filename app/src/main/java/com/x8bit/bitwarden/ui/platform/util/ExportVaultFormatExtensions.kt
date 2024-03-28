package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat

/**
 *  Provides a human-readable label for the export format.
 */
val ExportVaultFormat.displayLabel: Text
    get() = when (this) {
        ExportVaultFormat.JSON -> R.string.json_extension.asText()
        ExportVaultFormat.CSV -> R.string.csv_extension.asText()
        ExportVaultFormat.JSON_ENCRYPTED -> R.string.json_extension_formatted.asText(
            R.string.password_protected.asText(),
        )
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
