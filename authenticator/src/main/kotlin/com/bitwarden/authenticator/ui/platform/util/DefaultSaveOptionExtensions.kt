package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Returns a human-readable display label for the given [DefaultSaveOption].
 */
val DefaultSaveOption.displayLabel: Text
    get() = when (this) {
        DefaultSaveOption.NONE -> BitwardenString.none.asText()
        DefaultSaveOption.LOCAL -> BitwardenString.save_here.asText()
        DefaultSaveOption.BITWARDEN_APP -> BitwardenString.save_to_bitwarden.asText()
    }
