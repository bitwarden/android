package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText

/**
 *  Provides a human-readable label for the export format.
 */
val ImportFileFormat.displayLabel: Text
    get() = when (this) {
        ImportFileFormat.BITWARDEN_JSON -> R.string.import_format_label_bitwarden_json.asText()
        ImportFileFormat.TWO_FAS_JSON -> R.string.import_format_label_2fas_json.asText()
        ImportFileFormat.LAST_PASS_JSON -> R.string.import_format_label_lastpass_json.asText()
        ImportFileFormat.AEGIS -> R.string.import_format_label_aegis_json.asText()
    }
