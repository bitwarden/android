package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment

/**
 * Returns a human-readable display label for the given [Environment.Type].
 */
val Environment.Type.displayLabel: Text
    get() = when (this) {
        Environment.Type.US -> Environment.Us.label.asText()
        Environment.Type.EU -> Environment.Eu.label.asText()
        Environment.Type.SELF_HOSTED -> R.string.self_hosted.asText()
    }
