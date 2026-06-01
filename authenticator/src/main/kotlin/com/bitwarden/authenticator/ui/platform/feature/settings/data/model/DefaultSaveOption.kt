package com.bitwarden.authenticator.ui.platform.feature.settings.data.model

/**
 * Represents the default save location the user has set.
 *
 * The [value] is used for consistent storage purposes.
 */
enum class DefaultSaveOption(val value: String?) {
    BITWARDEN_APP(value = "bitwarden"),
    LOCAL(value = "local"),
    NONE(value = null),
}
