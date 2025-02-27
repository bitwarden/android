package com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model

/**
 * Represents the theme options the user can set.
 *
 * The [value] is used for consistent storage purposes.
 */
enum class AppTheme(val value: String?) {
    DEFAULT(value = null),
    DARK(value = "dark"),
    LIGHT(value = "light"),
}
