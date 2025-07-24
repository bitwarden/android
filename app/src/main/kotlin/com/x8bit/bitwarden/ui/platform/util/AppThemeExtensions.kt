package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Returns a human-readable display label for the given [AppTheme].
 */
val AppTheme.displayLabel: Text
    get() = when (this) {
        AppTheme.DEFAULT -> BitwardenString.default_system.asText()
        AppTheme.DARK -> BitwardenString.dark.asText()
        AppTheme.LIGHT -> BitwardenString.light.asText()
    }
