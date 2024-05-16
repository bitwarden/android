package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportFormat

/**
 *  Provides a human-readable label for the export format.
 */
val ExportFormat.displayLabel: Text
    get() = when (this) {
        ExportFormat.JSON -> R.string.export_format_label_json.asText()
        ExportFormat.CSV -> R.string.export_format_label_csv.asText()
    }

/**
 * Provides the file extension associated with the export format.
 */
val ExportFormat.fileExtension: String
    get() = when (this) {
        ExportFormat.JSON -> "json"
        ExportFormat.CSV -> "csv"
    }
