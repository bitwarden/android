package com.x8bit.bitwarden.ui.autofill.util

import android.content.Context
import android.content.res.Configuration

/**
 * Whether or not dark mode is currently active at the system level.
 */
val Context.isSystemDarkMode: Boolean
    get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
