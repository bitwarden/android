package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption

/**
 * Returns a human-readable display label for the given [DefaultSaveOption].
 */
val DefaultSaveOption.displayLabel: Text
    get() = when (this) {
        DefaultSaveOption.NONE -> R.string.none.asText()
        DefaultSaveOption.LOCAL -> R.string.save_locally.asText()
        DefaultSaveOption.BITWARDEN_APP -> R.string.save_to_bitwarden.asText()
    }
