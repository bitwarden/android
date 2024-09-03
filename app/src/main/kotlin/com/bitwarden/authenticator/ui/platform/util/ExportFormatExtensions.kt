package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat

/**
 *  Provides a human-readable label for the export format.
 */
val ExportVaultFormat.displayLabel: Text
    get() = when (this) {
        ExportVaultFormat.JSON -> R.string.export_format_label_json.asText()
        ExportVaultFormat.CSV -> R.string.export_format_label_csv.asText()
    }

/**
 * Provides the file extension associated with the export format.
 */
val ExportVaultFormat.fileExtension: String
    get() = when (this) {
        ExportVaultFormat.JSON -> "json"
        ExportVaultFormat.CSV -> "csv"
    }
