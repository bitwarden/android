package com.bitwarden.ui.platform.util

import android.content.res.Configuration

/**
 * Convenience method to check if the system is currently in dark mode.
 */
val Configuration.isSystemInDarkMode
    get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
