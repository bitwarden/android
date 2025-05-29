package com.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme

/**
 * Returns `true` if the app is currently using dark mode.
 */
fun AppTheme.isDarkMode(
    isSystemDarkMode: Boolean,
): Boolean =
    when (this) {
        AppTheme.DEFAULT -> isSystemDarkMode
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }
