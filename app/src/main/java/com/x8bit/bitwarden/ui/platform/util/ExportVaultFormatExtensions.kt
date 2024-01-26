package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat

val ExportVaultFormat.displayLabel: String
    get() = when (this) {
        ExportVaultFormat.JSON -> ".json"
        ExportVaultFormat.CSV -> ".csv"
        ExportVaultFormat.JSON_ENCRYPTED -> ".json (Encrypted)"
    }
