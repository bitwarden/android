package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.importing.model.ImportFormat

/**
 *  Provides a human-readable label for the export format.
 */
val ImportFormat.displayLabel: Text
    get() = when (this) {
        ImportFormat.JSON -> R.string.json_extension.asText()
    }

/**
 * Provides the file extension associated with the export format.
 */
val ImportFormat.fileExtension: String
    get() = when (this) {
        ImportFormat.JSON -> "json"
    }
