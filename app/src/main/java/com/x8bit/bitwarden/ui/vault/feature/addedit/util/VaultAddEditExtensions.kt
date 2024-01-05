package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat

/**
 * Default, "select" Text to show on multi select buttons in the VaultAddEdit package.
 */
val SELECT_TEXT: Text
    get() = "-- "
        .asText()
        .concat(R.string.select.asText())
        .concat(" --".asText())
