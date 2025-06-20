package com.bitwarden.ui.platform.feature.settings.appearance.model

import androidx.appcompat.app.AppCompatDelegate

/**
 * Represents the theme options the user can set.
 *
 * The [value] is used for consistent storage purposes.
 */
enum class AppTheme(val value: String?, val osValue: Int) {
    DEFAULT(value = null, osValue = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DARK(value = "dark", osValue = AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT(value = "light", osValue = AppCompatDelegate.MODE_NIGHT_NO),
}
